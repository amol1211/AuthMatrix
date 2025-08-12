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
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.MalformedJwtException;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final AppUserDetailsService appUserDetailsService;
    private final JwtUtil jwtUtil;

    private static final List<String> PUBLIC_URL_PREFIXES = List.of(
            "/", "/index.html", "/favicon.ico", "/favicon.png", "/assets/",
            "/manifest.json", "/logo192.png", "/logo512.png",
            "/register", "/login", "/verify-otp", "/is-authenticated",
            "/send-reset-otp", "/reset-password", "/logout", "/debug-auth"
           
    );

    private String extractJwtFromRequest(HttpServletRequest request) {
        // 1) Check Authorization header
        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            logger.info("Found Authorization header with Bearer token");
            return authorizationHeader.substring(7);
        }

        // 2) Check cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    logger.info("Found jwt cookie");
                    return cookie.getValue();
                }
            }
        }

        // 3) No token found
        return null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // Mark as public if it matches known prefixes or is a static asset
        boolean isPublic = PUBLIC_URL_PREFIXES.stream()
                .anyMatch(prefix -> requestUri.equals(prefix) || requestUri.startsWith(prefix))
                || requestUri.matches(".*\\.(js|css|png|svg|woff2|ttf)$");

        logger.debug("Processing request URI: " + requestUri + " | isPublic: " + isPublic);

        if (isPublic) {
            // For public endpoints, just continue filter chain without auth check.
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = extractJwtFromRequest(request);
        if (jwt == null) {
            logger.warn("No JWT found for protected route: " + requestUri);
            SecurityContextHolder.clearContext();  // Clear context if no token
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtUtil.extractEmail(jwt);
            logger.debug("Email extracted from token: " + email);

            // Defensive check: reject "anonymousUser"
            if (email == null || email.equalsIgnoreCase("anonymousUser")) {
                logger.warn("Extracted email is null or anonymousUser - rejecting token");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = appUserDetailsService.loadUserByUsername(email);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("JWT validated and SecurityContext set for user: " + email);
                } else {
                    logger.warn("JWT validation failed for user: " + email);
                    SecurityContextHolder.clearContext();
                }
            } else {
                logger.debug("User already authenticated in SecurityContext.");
            }
        } catch (ExpiredJwtException eje) {
            logger.warn("Token expired: " + eje.getMessage(), eje);
            SecurityContextHolder.clearContext();
        } catch (SignatureException sie) {
            logger.warn("Invalid JWT signature: " + sie.getMessage(), sie);
            SecurityContextHolder.clearContext();
        } catch (MalformedJwtException mje) {
            logger.warn("Malformed JWT: " + mje.getMessage(), mje);
            SecurityContextHolder.clearContext();
        } catch (Exception ex) {
            logger.error("Unexpected JWT processing error for " + requestUri + ": " + ex.getMessage(), ex);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
