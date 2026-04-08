package com.fooddelivery.payments.service;

import java.util.UUID;

public record PaymentCompletedEvent(UUID orderId, UUID paymentId) {}
