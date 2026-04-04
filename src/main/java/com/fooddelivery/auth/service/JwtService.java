package com.fooddelivery.auth.service;

import com.fooddelivery.auth.entity.RefreshToken;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.repository.RefreshTokenRepository;
import com.fooddelivery.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class JwtService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Value("${jwt.secret}")
    private String secret;

    public JwtService(RefreshTokenRepository refreshTokenRepository,
                      TokenBlacklistService tokenBlacklistService,
                      PasswordEncoder passwordEncoder,
                      UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("forcePasswordChange", user.isForcePasswordChange());
        if (user.getRole() == Role.CAFE_ADMIN && user.getCafeId() != null) {
            claims.put("cafeId", user.getCafeId().toString());
        }

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .addClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15))
                .signWith(getKey())
                .compact();
    }

    @Transactional
    public String createRefreshToken(User user, String deviceId, String userAgent, String ip) {
        if (!user.isActive()) {
            throw new IllegalStateException("User account is deactivated");
        }

        List<RefreshToken> activeTokens =
                refreshTokenRepository.findAllByUserIdAndRevokedFalseOrderByCreatedAtAsc(user.getId());
        final int MAX_DEVICES = 5;
        if (activeTokens.size() >= MAX_DEVICES) {
            RefreshToken oldest = activeTokens.get(0);
            oldest.setRevoked(true);
            refreshTokenRepository.save(oldest);
        }

        String rawToken = UUID.randomUUID().toString();
        UUID tokenId = UUID.randomUUID();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(tokenId);
        refreshToken.setToken(passwordEncoder.encode(rawToken));
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(30));
        refreshToken.setRevoked(false);
        refreshToken.setDeviceId(deviceId);
        refreshToken.setUserAgent(userAgent);
        refreshToken.setIpAddress(ip);

        refreshTokenRepository.save(refreshToken);

        return tokenId + ":" + rawToken;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTempToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return Boolean.TRUE.equals(claims.get("temp"));
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            if (tokenBlacklistService.isBlacklisted(token)) {
                return false;
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (claims.getExpiration().before(new Date())) {
                return false;
            }

            String userId = claims.getSubject();
            User user = userRepository.findById(UUID.fromString(userId))
                    .orElse(null);

            return user != null && user.isActive();

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public RefreshToken validateRefreshToken(String tokenWithId) {
        String[] parts = tokenWithId.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid refresh token format");
        }

        UUID tokenId = UUID.fromString(parts[0]);
        String rawToken = parts[1];

        RefreshToken rt = refreshTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (rt.isRevoked()) {
            throw new IllegalArgumentException("Refresh token already revoked");
        }
        if (rt.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }
        if (!passwordEncoder.matches(rawToken, rt.getToken())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        if (!rt.getUser().isActive()) {
            throw new IllegalArgumentException("User account is deactivated");
        }

        return rt;
    }

    public long getRemainingTime(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }

    @Transactional
    public Map<String, String> refreshTokens(String tokenWithId,
                                             String deviceId, String userAgent, String ip) {
        RefreshToken existingToken = validateRefreshToken(tokenWithId);

        try {
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);
        } catch (OptimisticLockException e) {
            throw new IllegalStateException("Refresh token already used");
        }

        User user = existingToken.getUser();
        String newRefreshToken = createRefreshToken(user, deviceId, userAgent, ip);
        String newAccessToken = generateAccessToken(user);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);
        return tokens;
    }
}