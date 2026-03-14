package com.fooddelivery.orders.service;

import com.fooddelivery.orders.dto.CreatePaymentRequest;
import com.fooddelivery.orders.dto.PaymentResponse;
import com.fooddelivery.orders.entity.Payment;
import com.fooddelivery.orders.entity.PaymentStatus;
import com.fooddelivery.orders.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setClientId(request.getClientId());
        payment.setAmount(request.getAmount());
        payment.setPlatformFee(request.getPlatformFee() != null ? request.getPlatformFee() : BigDecimal.ZERO);
        payment.setStatus(PaymentStatus.PENDING);

        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved);
    }

    public PaymentResponse updateStatus(UUID paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(status);
        Payment updated = paymentRepository.save(payment);
        return mapToResponse(updated);
    }

    public List<PaymentResponse> getPaymentsByClient(UUID clientId) {
        return paymentRepository.findByClientId(clientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPaymentsByOrder(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setOrderId(payment.getOrderId());
        response.setClientId(payment.getClientId());
        response.setAmount(payment.getAmount());
        response.setPlatformFee(payment.getPlatformFee());
        response.setStatus(payment.getStatus());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }
}