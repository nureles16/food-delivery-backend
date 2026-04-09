package com.fooddelivery.mapper;

import com.fooddelivery.payments.dto.PaymentResponse;
import com.fooddelivery.payments.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) return null;
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setOrderId(payment.getOrderId());
        response.setClientId(payment.getClientId());
        response.setAmount(payment.getAmount());
        response.setPlatformFee(payment.getPlatformFee());
        response.setDeliveryFee(payment.getDeliveryFee());
        response.setRestaurantPayout(payment.getRestaurantPayout());
        response.setStatus(payment.getStatus());
        response.setPayoutStatus(payment.getPayoutStatus());
        response.setProviderPaymentId(payment.getProviderPaymentId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }
}