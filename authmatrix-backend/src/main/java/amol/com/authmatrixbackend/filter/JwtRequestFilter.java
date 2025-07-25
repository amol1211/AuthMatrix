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
            "/send-reset-otp", "/reset-password", "/logout"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        boolean isPublicUrl = false;

        for (String prefix : PUBLIC_URL_PREFIXES) {
            if (requestUri.equals(prefix) || requestUri.startsWith(prefix)) {
                isPublicUrl = true;
                break;
            }
        }

        if (!isPublicUrl && (
                requestUri.endsWith(".js") || requestUri.endsWith(".css") ||
                requestUri.endsWith(".png") || requestUri.endsWith(".svg") ||
                requestUri.endsWith(".woff2") || requestUri.endsWith(".ttf")
        )) {
            isPublicUrl = true;
        }

        if (isPublicUrl) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = null;
        String email = null;

        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
        }

        if (jwt == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        break;
                    }
                }
            }
        }

        if (jwt != null) {
            try {
                email = jwtUtil.extractEmail(jwt);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = appUserDetailsService.loadUserByUsername(email);
                    if (jwtUtil.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        //Success log
                        System.out.println("JWT validated for user: " + email);
                    }
                }

            } catch (ExpiredJwtException e) {
                System.out.println("JWT expired: " + e.getMessage());
            } catch (SignatureException | MalformedJwtException | IllegalArgumentException e) {
                System.out.println("Invalid JWT: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("JWT processing failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        filterChain.doFilter(request, response);
    }
}
