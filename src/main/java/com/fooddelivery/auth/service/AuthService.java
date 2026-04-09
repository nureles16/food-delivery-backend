package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.entity.PasswordResetToken;
import com.fooddelivery.auth.entity.RefreshToken;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.repository.PasswordResetTokenRepository;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.exceptions.*;
import com.fooddelivery.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       TokenBlacklistService tokenBlacklistService,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userMapper = userMapper;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Phone already exists");
        }

        User user = userMapper.toUser(request);
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
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid password");
        }

        if (!user.isActive()) {
            throw new ForbiddenException("Account is disabled");
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
            throw new ForbiddenException("Only SUPER_ADMIN can perform this action");
        }
    }

    @Transactional
    public void createCafeAdmin(CreateCafeAdminRequest request) {
        checkSuperAdminRole();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Phone already exists");
        }

        User user = userMapper.toUser(request);
        String tempPassword = generateRandomPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setRole(Role.CAFE_ADMIN);
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
                .orElseThrow(() -> new NotFoundException("User not found with email: " + request.getEmail()));

        if (!user.isActive()) {
            throw new ForbiddenException("Account is disabled. Cannot reset password.");
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
                .orElseThrow(() -> new NotFoundException("Invalid password reset token"));

        if (resetToken.isUsed()) {
            throw new ConflictException("Token already used");
        }
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token expired");
        }

        User user = resetToken.getUser();
        if (!user.isActive()) {
            throw new ForbiddenException("Account is disabled");
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
                .orElseThrow(() -> new NotFoundException("User not found"));
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
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
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
            throw new BadRequestException("Invalid access token");
        }
        UUID tokenUserId = UUID.fromString(userIdStr);

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("Current user not found"));

        if (!currentUser.getId().equals(tokenUserId)) {
            log.warn("Logout attempt with token belonging to different user: tokenUserId={}, currentUserId={}",
                    tokenUserId, currentUser.getId());
            throw new ForbiddenException("Token does not belong to the authenticated user");
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
            throw new UnauthorizedException("User not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new UnauthorizedException("Principal is not UserDetails");
        }
        String email = ((UserDetails) principal).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Authenticated user not found in database"));
    }

    public UserDetails getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new UnauthorizedException("Principal is not UserDetails");
        }
        return (UserDetails) principal;
    }
}