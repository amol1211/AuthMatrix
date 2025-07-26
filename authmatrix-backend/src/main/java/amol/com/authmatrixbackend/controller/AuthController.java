package amol.com.authmatrixbackend.controller;

import amol.com.authmatrixbackend.io.AuthRequest;
import amol.com.authmatrixbackend.io.ResetPasswordRequest;
import amol.com.authmatrixbackend.service.AppUserDetailsService;
import amol.com.authmatrixbackend.service.ProfileService;
import amol.com.authmatrixbackend.util.JwtUtil;
import amol.com.authmatrixbackend.entity.UserEntity;
import amol.com.authmatrixbackend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.security.core.Authentication;

import jakarta.servlet.http.Cookie;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserDetailsService appUserDetailsService;
    private final JwtUtil jwtUtil;
    private final ProfileService profileService;
    private final UserRepository userRepository; // ✅ Injected UserRepository

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            authenticate(request.getEmail(), request.getPassword());

            final UserDetails userDetails = appUserDetailsService.loadUserByUsername(request.getEmail());
            final String jwtToken = jwtUtil.generateToken(userDetails);
            UserEntity user = userRepository.findByEmail(request.getEmail()).get();

            Map<String, Object> userData = new HashMap<>();
            userData.put("email", user.getEmail());
            userData.put("name", user.getName());
            userData.put("isAccountVerified", user.getIsAccountVerified()); // ✅ Make sure getter is correct

            ResponseCookie cookie = ResponseCookie.from("jwt", jwtToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("lax") 
                    .path("/")
                    .maxAge(Duration.ofDays(1))
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(userData);

        } catch (BadCredentialsException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "Email or Password is incorrect");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (DisabledException ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "User account is disabled");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception ex) {
            System.err.println("Unexpected login error: " + ex.getMessage());
            ex.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "Authentication failed: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    private void authenticate(String email, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
    }

    @GetMapping("/is-authenticated") 
    public ResponseEntity<Boolean> isAuthenticated(@CurrentSecurityContext(expression = "authentication?.name") String email) {
        return ResponseEntity.ok(email != null);
    }

    @PostMapping("/send-reset-otp")
    public void sendResetOtp(@RequestParam String email) {
        try {
            profileService.sendResetOtp(email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            profileService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

/*     @PostMapping("/send-otp")
    public void sendVerifyOtp(@CurrentSecurityContext(expression = "authentication?.name") String email) {
        System.out.println("➡️ /send-otp called by user: " + email);
        try {
            profileService.sendOtp(email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    } */

    /* @PostMapping("/send-otp")
public void sendVerifyOtp() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String email = auth != null ? auth.getName() : null;
    System.out.println("➡️ /send-otp called by user: " + email);

    if (email == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
    }

    try {
        profileService.sendOtp(email);
    } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
} */


   // Replace your existing /send-otp method with this:
@PostMapping("/send-otp")
public ResponseEntity<?> sendVerifyOtp(HttpServletRequest request) {
    // Debug logging
    System.out.println("/send-otp endpoint called");
    
    // Check cookies
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if ("jwt".equals(cookie.getName())) {
                System.out.println("JWT Cookie found: " + cookie.getValue().substring(0, Math.min(50, cookie.getValue().length())) + "...");
            }
        }
    } else {
        System.out.println("No cookies found in request");
    }
    
    // Check authentication
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    System.out.println("Authentication object: " + auth);
    System.out.println("Is authenticated: " + (auth != null && auth.isAuthenticated()));
    System.out.println("Principal: " + (auth != null ? auth.getPrincipal() : "null"));
    
    String email = auth != null ? auth.getName() : null;
    System.out.println("📧 Extracted email: " + email);

    if (email == null || auth == null || !auth.isAuthenticated()) {
        System.out.println("Authentication failed - returning 401");
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", "User not authenticated - please log in again");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    try {
        System.out.println("Calling profileService.sendOtp for: " + email);
        profileService.sendOtp(email);
        System.out.println("OTP sent successfully for: " + email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "OTP sent successfully");
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        System.err.println("Error in sendOtp: " + e.getMessage());
        e.printStackTrace();
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", "Failed to send OTP: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

// Add this new debug method to your AuthController:
@GetMapping("/debug-auth")
public ResponseEntity<?> debugAuth(HttpServletRequest request) {
    System.out.println("/debug-auth endpoint called");
    
    Map<String, Object> debugInfo = new HashMap<>();
    
    // Check cookies
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if ("jwt".equals(cookie.getName())) {
                debugInfo.put("jwtCookieFound", true);
                debugInfo.put("jwtPreview", cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "...");
                
                // Try to validate the JWT
                try {
                    String email = jwtUtil.extractEmail(cookie.getValue());
                    debugInfo.put("emailFromJWT", email);
                    
                    UserDetails userDetails = appUserDetailsService.loadUserByUsername(email);
                    boolean isValid = jwtUtil.validateToken(cookie.getValue(), userDetails);
                    debugInfo.put("jwtValid", isValid);
                } catch (Exception e) {
                    debugInfo.put("jwtError", e.getMessage());
                }
            }
        }
    } else {
        debugInfo.put("jwtCookieFound", false);
    }
    
    // Check authentication
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    debugInfo.put("authenticationExists", auth != null);
    debugInfo.put("isAuthenticated", auth != null && auth.isAuthenticated());
    debugInfo.put("principalType", auth != null ? auth.getPrincipal().getClass().getSimpleName() : "null");
    debugInfo.put("email", auth != null ? auth.getName() : "null");
    
    return ResponseEntity.ok(debugInfo);
}


    @PostMapping("/verify-otp")
    public void verifyEmail(@RequestBody Map<String, Object> request, @CurrentSecurityContext(expression = "authentication?.name") String email) {
        if (request.get("otp") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP is required");
        }
        try {
            profileService.verifyOtp(email, request.get("otp").toString());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("lax") 
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logged out successfully");
    }
}
