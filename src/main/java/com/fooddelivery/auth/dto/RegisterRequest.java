package com.fooddelivery.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные для регистрации нового пользователя")
public class RegisterRequest {

    @Schema(description = "Email пользователя", example = "user@example.com")
    @Email(message = "Некорректный email")
    @NotBlank(message = "Email обязателен")
    @Size(max = 255)
    private String email;

    @Schema(description = "Пароль пользователя", example = "StrongP@ssword1")
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 100)
    private String password;

    @Schema(description = "Телефон пользователя", example = "+996700123456")
    @Pattern(
            regexp = "^\\+\\d{10,15}$",
            message = "Телефон должен быть в формате +996XXXXXXXXX"
    )
    private String phone;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}