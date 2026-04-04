package com.fooddelivery.auth.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateUserStatusRequest {
    @NotNull
    private Boolean isActive;

    public UpdateUserStatusRequest(Boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isActive() {
        return isActive;
    }
}