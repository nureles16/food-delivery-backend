package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.entity.PasswordResetToken;
import com.fooddelivery.auth.entity.RefreshToken;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.repository.PasswordResetTokenRepository;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository, TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CLIENT);
        user.setActive(true);
        user.setForcePasswordChange(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String deviceId, String userAgent, String ip) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is disabled");
        }
        String accessToken = jwtService.generateAccessToken(user);

        String refreshToken = jwtService.createRefreshToken(user, deviceId, userAgent, ip);

        boolean forcePasswordChange = user.isForcePasswordChange();
        return new AuthResponse(accessToken, refreshToken, forcePasswordChange);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue, String deviceId, String userAgent, String ip) {
        var tokens = jwtService.refreshTokens(refreshTokenValue, deviceId, userAgent, ip);
        String newAccessToken = tokens.get("accessToken");
        String newRefreshToken = tokens.get("refreshToken");
        User user = jwtService.validateRefreshToken(refreshTokenValue).getUser();
        boolean forcePasswordChange = user.isForcePasswordChange();
        return new AuthResponse(newAccessToken, newRefreshToken, forcePasswordChange);
    }

    private void checkSuperAdminRole() {
        UserDetails currentUser = getCurrentUserDetails();
        boolean isSuperAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (!isSuperAdmin) {
            throw new AccessDeniedException("Only SUPER_ADMIN can perform this action");
        }
    }
    @Transactional
    public void createCafeAdmin(CreateCafeAdminRequest request) {
        checkSuperAdminRole();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        String tempPassword = generateRandomPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setRole(Role.CAFE_ADMIN);
        user.setCafeId(request.getCafeId());
        user.setActive(true);
        user.setForcePasswordChange(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        log.info("Created cafe admin: {}", user.getEmail());
        System.out.println("===== В РЕАЛЬНОЙ СИСТЕМЕ ЭТО БЫЛО БЫ ОТПРАВЛЕНО НА EMAIL =====");
        System.out.println("Email: " + user.getEmail() + ", временный пароль: " + tempPassword);
        System.out.println("==================================================================");
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getEmail()));

        if (!user.isActive()) {
            throw new IllegalStateException("Account is disabled. Cannot reset password.");
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(1));
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset token generated for user: {}", user.getEmail());
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid password reset token"));

        if (resetToken.isUsed()) {
            throw new RuntimeException("Token already used");
        }
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        User user = resetToken.getUser();
        if (!user.isActive()) {
            throw new RuntimeException("Account is disabled");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setForcePasswordChange(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        refreshTokenRepository.revokeAllUserTokens(user.getId());
    }

    @Transactional
    public void updateUserStatus(UUID userId, boolean active) {
        checkSuperAdminRole();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(active);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        if (!active) {
            refreshTokenRepository.revokeAllUserTokens(user.getId());
        }
    }

    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setForcePasswordChange(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenRepository.revokeAllUserTokens(user.getId());
    }

    @Transactional
    public void logout(String accessToken, String currentUserEmail) {
        String userIdStr;
        try {
            userIdStr = jwtService.extractUserId(accessToken);
        } catch (Exception e) {
            log.warn("Cannot extract userId from token during logout: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid access token");
        }
        UUID tokenUserId = UUID.fromString(userIdStr);

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        if (!currentUser.getId().equals(tokenUserId)) {
            log.warn("Logout attempt with token belonging to different user: tokenUserId={}, currentUserId={}",
                    tokenUserId, currentUser.getId());
            throw new SecurityException("Token does not belong to the authenticated user");
        }

        if (jwtService.validateToken(accessToken)) {
            long remainingTimeMs = jwtService.getRemainingTime(accessToken);
            tokenBlacklistService.blacklistToken(accessToken, remainingTimeMs);
        }

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(currentUser.getId());
        for (RefreshToken rt : tokens) {
            rt.setRevoked(true);
        }
        refreshTokenRepository.saveAll(tokens);

        log.info("User {} logged out, all tokens revoked", currentUser.getId());
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new RuntimeException("Principal is not UserDetails");
        }
        String email = ((UserDetails) principal).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }

    public UserDetails getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new RuntimeException("Principal is not UserDetails");
        }
        return (UserDetails) principal;
    }
}