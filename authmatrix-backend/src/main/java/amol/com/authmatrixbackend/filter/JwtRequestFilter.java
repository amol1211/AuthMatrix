package amol.com.authmatrixbackend.filter;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import amol.com.authmatrixbackend.service.AppUserDetailsService;
import amol.com.authmatrixbackend.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;

import lombok.RequiredArgsConstructor;
import io.jsonwebtoken.ExpiredJwtException; // Import for JWT exception handling
import io.jsonwebtoken.SignatureException; // Import for JWT exception handling
import io.jsonwebtoken.MalformedJwtException; // Import for JWT exception handling


@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
    
    private final AppUserDetailsService appUserDetailsService;
    private final JwtUtil jwtUtil;

    // This list MUST comprehensively match all permitAll() URLs from SecurityConfig.java
    // These are paths that should NOT require a valid JWT.
    private static final List<String> PUBLIC_URL_PREFIXES = List.of(
            "/",                          // Root path (e.g., your React app's base URL)
            "/index.html",                // Main HTML file
            "/favicon.ico",               // Standard favicon
            "/favicon.png",               // Another favicon common in projects
            "/assets/",                   // All static assets under the /assets/ directory
            "/manifest.json",             // Progressive Web App manifest file
            "/logo192.png",               // App logo for PWA
            "/logo512.png",               // App logo for PWA
            "/register",                  // User registration API endpoint
            "/login",                     // User login API endpoint
            "/verify-otp",                // Verify OTP API endpoint
            "/is-authenticated",          // Check authentication status (can be accessed without valid JWT)
            "/send-reset-otp",            // Send password reset OTP API endpoint
            "/reset-password",            // Reset password API endpoint
            "/logout"                     // Logout API endpoint (often handled without active JWT)
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI(); // Get the full request URI

        // Determine if the current request URI is a publicly accessible URL
        boolean isPublicUrl = false;
        for (String publicPrefix : PUBLIC_URL_PREFIXES) {
            // Check for exact match or if the URI starts with a public prefix
            if (requestUri.equals(publicPrefix) || requestUri.startsWith(publicPrefix)) {
                isPublicUrl = true;
                break; // Found a match, no need to check further
            }
        }
        
        // Additionally, check for common static file extensions
        // This is important because a path like "/main.js" might not start with a configured prefix like "/assets/"
        if (!isPublicUrl && (
            requestUri.endsWith(".js") ||
            requestUri.endsWith(".css") ||
            requestUri.endsWith(".png") ||
            requestUri.endsWith(".svg") ||
            requestUri.endsWith(".woff2") ||
            requestUri.endsWith(".ttf")
        )) {
            isPublicUrl = true;
        }

        // If the request is for a public URL, bypass JWT validation entirely
        if (isPublicUrl) {
            filterChain.doFilter(request, response);
            return; // IMPORTANT: Exit the method here, no further JWT processing needed
        }

        // --- Only proceed with JWT processing for non-public (potentially protected) URLs ---

        String jwt = null;
        String email = null;

        // 1. Try to extract JWT from the "Authorization" header (e.g., "Bearer <token>")
        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Extract the token part
        }

        // 2. If JWT not found in header, try to extract it from cookies
        if (jwt == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt".equals(cookie.getName())) { // Look for a cookie named "jwt"
                        jwt = cookie.getValue();
                        break;
                    }
                }
            }
        }

        // 3. If a JWT was found, attempt to validate it and set the security context
        if (jwt != null) {
            try {
                // Extract email from the JWT
                email = jwtUtil.extractEmail(jwt); 

                // If email is valid and no authentication is currently set in the context
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Load user details from your UserDetailsService
                    UserDetails userDetails = appUserDetailsService.loadUserByUsername(email);

                    // Validate the token against the user details
                    if (jwtUtil.validateToken(jwt, userDetails)) {
                        // If valid, create an authentication token and set it in the SecurityContextHolder
                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                }
            } catch (ExpiredJwtException e) {
                // Log expired JWTs. The CustomAuthenticationEntryPoint will handle the 401 response.
                System.err.println("JWT token expired: " + e.getMessage());
            } catch (SignatureException | MalformedJwtException | IllegalArgumentException e) {
                // Log other JWT validation failures (e.g., invalid signature, malformed token)
                System.err.println("Invalid JWT token: " + e.getMessage());
            } catch (Exception e) {
                // Catch any other unexpected exceptions during JWT processing
                System.err.println("Error processing JWT: " + e.getMessage());
                e.printStackTrace(); // Print full stack trace for debugging unexpected errors
            }
        }

        // Continue the filter chain to the next filter or the target servlet/controller
        filterChain.doFilter(request, response);
    }
}