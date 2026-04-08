package com.fooddelivery.payments.dto;

import java.util.UUID;



public class PaymentFailedEvent {
    private final UUID orderId;
    private final UUID paymentId;

    public PaymentFailedEvent(UUID orderId, UUID paymentId) {
        this.orderId = orderId;
        this.paymentId = paymentId;
    }

    public UUID getOrderId() { return orderId; }
    public UUID getPaymentId() { return paymentId; }
}