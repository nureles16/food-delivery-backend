package com.fooddelivery.orders.dto;

import jakarta.validation.constraints.NotBlank;

public class CancelOrderRequest {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}