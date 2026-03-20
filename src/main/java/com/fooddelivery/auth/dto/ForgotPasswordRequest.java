package com.fooddelivery.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на восстановление пароля")
public class ForgotPasswordRequest {

    @Schema(description = "Email пользователя", example = "user@mail.com")
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    private String email;
    // --- Геттеры и сеттеры ---
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}