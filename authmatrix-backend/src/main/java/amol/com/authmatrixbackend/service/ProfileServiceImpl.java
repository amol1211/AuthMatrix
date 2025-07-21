package amol.com.authmatrixbackend.service;

import amol.com.authmatrixbackend.entity.UserEntity;
import amol.com.authmatrixbackend.io.ProfileRequest;
import amol.com.authmatrixbackend.io.ProfileResponse;
import amol.com.authmatrixbackend.repository.UserRepository;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


// Service implementation for profile creation logic

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    @Override
    public ProfileResponse createProfile(ProfileRequest request) {

        // Convert request to entity, save to DB, then convert to response
        UserEntity newProfile = convertToUserEntity(request);

        if(!userRepository.existsByEmail(newProfile.getEmail())) {
            newProfile = userRepository.save(newProfile);
            return convertToProfileResponse(newProfile);
        }
        
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        
    }
    @Override
    public ProfileResponse getProfile(String email) {
        
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Profile not found: " + email));

        return convertToProfileResponse(existingUser);        
    }

    @Override
    public void sendResetOtp(String email) {
        
        UserEntity existingEntity =  userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Generating 6 digit OTP and set expiration time  
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        //Expires in (current time + 15 minutes in milliseconds)
        long expiryTime = System.currentTimeMillis() + (15 * 60 * 1000); 

        //Updating the user/profile
        existingEntity.setResetOtp(otp);
        existingEntity.setResetOtpExpireAt(expiryTime);

        //Saving into the database
        userRepository.save(existingEntity);

        try {
            //Sending the reset OTP email
           emailService.sendResetOtpEmail(existingEntity.getEmail(), otp); 
        
        } catch (Exception ex) {
            throw new RuntimeException("Unable to send reset OTP email");
        }
    }

    @Override
    public void resetPassword(String email, String otp, String newPassword) {

        UserEntity existingUser = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

            if (existingUser.getResetOtp() == null || !existingUser.getResetOtp().equals(otp)) {
                throw new RuntimeException("Invalid OTP");
            }

            if (existingUser.getResetOtpExpireAt() < System.currentTimeMillis()) {
                throw new RuntimeException("OTP has expired");
            }

            existingUser.setPassword(passwordEncoder.encode(newPassword));
            existingUser.setResetOtp(null); 
            existingUser.setResetOtpExpireAt(0L); // Reset OTP and expiration time
            userRepository.save(existingUser);
    }

    @Override
    public void sendOtp(String email) {

        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (existingUser.getIsAccountVerified() != null && existingUser.getIsAccountVerified()) {
            return;
        }

        //Generating 6 digit OTP

        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        //Expires in (current time + 24 hours in milliseconds)
        long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        
        //Updating the user/profile
        existingUser.setVerifyOtp(otp);     
        existingUser.setVerifyOtpExpireAt(expiryTime);

        //Saving into the database
        userRepository.save(existingUser);

        try {
            emailService.sendOtpEmail(existingUser.getEmail(), otp);
        } catch (Exception e) {
            
            throw new RuntimeException("Unable to send email"); 
        }
    }

    @Override
    public void verifyOtp(String email, String otp) {

        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (existingUser.getVerifyOtp() == null || !existingUser.getVerifyOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        if (existingUser.getVerifyOtpExpireAt() < System.currentTimeMillis()) {
            throw new RuntimeException("OTP has expired");
        }

        existingUser.setIsAccountVerified(true);
        existingUser.setVerifyOtp(null); 
        existingUser.setVerifyOtpExpireAt(0L); 
        userRepository.save(existingUser);
    }

    
    // Converts UserEntity to ProfileResponse DTO
    private ProfileResponse convertToProfileResponse(UserEntity newProfile) {
        return ProfileResponse.builder()
                .name(newProfile.getName())
                .email(newProfile.getEmail())
                .userId(newProfile.getUserId())
                .isAccountVerified(newProfile.getIsAccountVerified())
                .build();

    }

    // Converts incoming request to a new UserEntity
    private UserEntity convertToUserEntity(ProfileRequest request) {
        return UserEntity.builder()
                .email(request.getEmail())
                .userId(UUID.randomUUID().toString())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .isAccountVerified(false)
                .resetOtpExpireAt(0L)
                .verifyOtp(null)
                .verifyOtpExpireAt(0L)
                .resetOtp(null)
                .build();
    }
}
