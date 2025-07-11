package amol.com.AuthMatrix.service;

import amol.com.AuthMatrix.entity.UserEntity;
import amol.com.AuthMatrix.io.ProfileRequest;
import amol.com.AuthMatrix.io.ProfileResponse;
import amol.com.AuthMatrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Service implementation for profile creation logic

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService{

    private final UserRepository userRepository;
    
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
                .password(request.getPassword())
                .isAccountVerified(false)
                .resetOtpExpireAt(0L)
                .verifyOtp(null)
                .verifyOtpExpireAt(0L)
                .resetOtp(null)
                .build();
    }
}
