package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refreshToken(RefreshRequest request) {

        String email = jwtService.extractEmail(request.getRefreshToken());

        String newAccessToken = jwtService.generateAccessToken(email);

        return new AuthResponse(newAccessToken, request.getRefreshToken());
    }
}