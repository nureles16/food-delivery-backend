package com.fooddelivery.auth.dto;

import jakarta.validation.constraints.NotNull;

public class StatusUpdateRequest {
    @NotNull
    private Boolean active;

    public Boolean isActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}