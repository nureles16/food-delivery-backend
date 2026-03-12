package com.fooddelivery.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Запрос на обновление Access Token по Refresh Token")
public class RefreshRequest {

    @Schema(description = "JWT Refresh токен", example = "dGhpc2lzdGhlcmVmcmVzaHRva2Vu...")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}