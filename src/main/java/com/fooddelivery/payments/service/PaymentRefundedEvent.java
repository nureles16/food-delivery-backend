package com.fooddelivery.payments.service;

import java.util.UUID;

public record PaymentRefundedEvent(UUID orderId, UUID paymentId) {}
