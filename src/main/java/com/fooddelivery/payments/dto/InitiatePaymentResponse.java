package com.fooddelivery.payments.dto;

import java.util.UUID;

public class InitiatePaymentResponse {
    private UUID paymentId;
    private String paymentUrl;

    public InitiatePaymentResponse(UUID paymentId, String paymentUrl) {
        this.paymentId = paymentId;
        this.paymentUrl = paymentUrl;
    }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
}