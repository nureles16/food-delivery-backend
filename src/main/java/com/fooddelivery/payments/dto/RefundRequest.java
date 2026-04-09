package com.fooddelivery.payments.dto;

import jakarta.validation.constraints.NotBlank;

public class RefundRequest {
    @NotBlank(message = "Cancelled reason is required")
    private String cancelledReason;

    public String getCancelledReason() {
        return cancelledReason;
    }

    public void setCancelledReason(String cancelledReason) {
        this.cancelledReason = cancelledReason;
    }
}