package com.fooddelivery.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateCafeAdminRequest {

    @NotBlank
    @Email
    private String email;

    private String phone;

    @NotNull
    private UUID cafeId;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public UUID getCafeId() { return cafeId; }
    public void setCafeId(UUID cafeId) { this.cafeId = cafeId; }
}