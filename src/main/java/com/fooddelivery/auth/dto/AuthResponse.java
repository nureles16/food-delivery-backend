package com.fooddelivery.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ аутентификации с токенами")
public class AuthResponse {

    @Schema(description = "JWT Access токен", example = "eyJhbGciOiJIUzI1...")
    private String accessToken;
    @Schema(description = "JWT Refresh токен", example = "dGhpc2lzdGhlcmVmcmVzaHRva2Vu...")
    private String refreshToken;
    private boolean forcePasswordChange;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken, boolean forcePasswordChange) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.forcePasswordChange = forcePasswordChange;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}