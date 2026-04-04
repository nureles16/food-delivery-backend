package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Auth", description = "API для регистрации, входа, обновления токена и управления паролем")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Регистрация нового пользователя")
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @Operation(summary = "Вход пользователя")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest httpRequest) {

        String userAgent = httpRequest.getHeader("User-Agent");
        String ip = httpRequest.getRemoteAddr();

        String finalDeviceId = (deviceId != null && !deviceId.isBlank())
                ? deviceId
                : generateDeviceId(userAgent, ip);

        AuthResponse response = authService.login(request, finalDeviceId, userAgent, ip);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Обновление Access токена")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest httpRequest) {

        String userAgent = httpRequest.getHeader("User-Agent");
        String ip = httpRequest.getRemoteAddr();

        String finalDeviceId = (deviceId != null && !deviceId.isBlank())
                ? deviceId
                : generateDeviceId(userAgent, ip);

        AuthResponse response = authService.refreshToken(request.getRefreshToken(), finalDeviceId, userAgent, ip);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Запрос на сброс пароля")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok("If the email exists, a reset link has been sent");
    }

    @Operation(summary = "Сброс пароля")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully");
    }

    @Operation(summary = "Выход пользователя (требуется access token в заголовке Authorization)")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> logout(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }
        String accessToken = authHeader.substring(7);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        String currentUserEmail = authentication.getName();

        authService.logout(accessToken, currentUserEmail);
        return ResponseEntity.ok("Logged out successfully");
    }

    @Operation(summary = "Смена пароля авторизованным пользователем")
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @Operation(summary = "Создание аккаунта кафе-администратора (только SUPER_ADMIN)")
    @PostMapping("/admin/users/cafe-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> createCafeAdmin(@Valid @RequestBody CreateCafeAdminRequest request) {
        authService.createCafeAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Cafe admin created successfully");
    }

    @Operation(summary = "Активация / деактивация пользователя (только SUPER_ADMIN)")
    @PatchMapping("/admin/users/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        authService.updateUserStatus(id, request.isActive());
        return ResponseEntity.ok("User status updated");
    }

    private String generateDeviceId(String userAgent, String ip) {
        String source = (userAgent != null ? userAgent : "") + "|" + (ip != null ? ip : "") + "|foodDeliverySalt";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            return UUID.nameUUIDFromBytes(source.getBytes()).toString();
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}