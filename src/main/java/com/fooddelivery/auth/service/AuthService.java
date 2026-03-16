package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.entity.RefreshToken;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();

        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        String hash = passwordEncoder.encode(request.getPassword());
        user.setPasswordHash(hash);

        user.setRole(Role.CLIENT);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());

        RefreshToken refreshToken = jwtService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse refreshToken(RefreshRequest request) {
        String tokenValue = request.getRefreshToken();
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (token.isRevoked() || token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token revoked");
        }

        // ROTATION: создаём новый refresh token
        RefreshToken newToken = jwtService.createRefreshToken(token.getUser());
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        String newAccessToken = jwtService.generateAccessToken(token.getUser().getEmail());
        return new AuthResponse(newAccessToken, newToken.getToken());
    }

    public void logout(String refreshTokenValue) {

        RefreshToken token = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        token.setRevoked(true);

        refreshTokenRepository.save(token);
    }
}