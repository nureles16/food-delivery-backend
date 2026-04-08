package com.fooddelivery.payments.entity;

public enum PaymentStatus {
    CREATED,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED;

    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == REFUNDED;
    }
}