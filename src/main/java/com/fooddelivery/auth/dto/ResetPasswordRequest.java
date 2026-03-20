package com.fooddelivery.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на сброс пароля")
public class ResetPasswordRequest {

    @Schema(description = "Токен сброса пароля", example = "abc123token")
    @NotBlank(message = "Token обязателен")
    private String token;

    @Schema(description = "Новый пароль", example = "StrongPass123")
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}