package com.fooddelivery.config;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.security.CustomUserDetails;
import com.fooddelivery.auth.security.CustomUserDetailsService;
import com.fooddelivery.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String[] FORCE_CHANGE_ALLOWED_PATHS = {
            "/auth/change-password",
            "/auth/logout"
    };

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService,
                                   UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            if (!jwtService.validateToken(jwt)) {
                throw new JwtException("Invalid token");
            }

            String userIdStr = jwtService.extractUserId(jwt);
            User user = userRepository.findById(UUID.fromString(userIdStr))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (!user.isActive()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User account is deactivated");
                return;
            }

            boolean isForcePasswordChange = user.isForcePasswordChange();
            boolean isTempToken = jwtService.isTempToken(jwt);
            boolean isChangePasswordEndpoint = request.getMethod().equals("POST") &&
                    PATH_MATCHER.match("/auth/change-password", request.getRequestURI());
            boolean isAllowedForceChangeEndpoint = isAllowedEndpoint(request);

            if (isTempToken && !isChangePasswordEndpoint) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Temporary token can only be used to change password");
                return;
            }

            if (isForcePasswordChange && !isAllowedForceChangeEndpoint) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"PASSWORD_CHANGE_REQUIRED\"}");
                return;
            }

            UserDetails userDetails = new CustomUserDetails(user);
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("User {} authenticated successfully with role {}", user.getEmail(), user.getRole());

        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal authentication error");
            return;
        }

        filterChain.doFilter(request, response);
    }
    private boolean isAllowedEndpoint(HttpServletRequest request) {
        if (!request.getMethod().equals("POST")) {
            return false;
        }
        String requestUri = request.getRequestURI();
        for (String path : FORCE_CHANGE_ALLOWED_PATHS) {
            if (PATH_MATCHER.match(path, requestUri)) {
                return true;
            }
        }
        return false;
    }
}