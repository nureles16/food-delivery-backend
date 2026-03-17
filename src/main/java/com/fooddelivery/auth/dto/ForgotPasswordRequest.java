package com.fooddelivery.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequest {

    @NotBlank
    @Email
    private String email;

    // --- Геттеры и сеттеры ---
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}