package com.fooddelivery.payments.service;

import com.fooddelivery.payments.dto.CreatePaymentRequest;
import com.fooddelivery.payments.dto.PaymentResponse;
import com.fooddelivery.payments.entity.Payment;
import com.fooddelivery.payments.entity.PaymentStatus;
import com.fooddelivery.payments.repository.PaymentRepository;
import com.fooddelivery.payments.specification.PaymentSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    public Page<PaymentResponse> getPaymentsByClient(UUID clientId,
                                                     UUID orderId,
                                                     PaymentStatus status,
                                                     BigDecimal minAmount,
                                                     BigDecimal maxAmount,
                                                     LocalDateTime startDate,
                                                     LocalDateTime endDate,
                                                     Pageable pageable) {
        Specification<Payment> spec = Specification
                .where(PaymentSpecification.clientIdEquals(clientId))
                .and(PaymentSpecification.orderIdEquals(orderId))
                .and(PaymentSpecification.statusEquals(status))
                .and(PaymentSpecification.amountBetween(minAmount, maxAmount))
                .and(PaymentSpecification.createdAtBetween(startDate, endDate));

        return paymentRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    public Page<PaymentResponse> getPaymentsByOrder(UUID orderId,
                                                    PaymentStatus status,
                                                    BigDecimal minAmount,
                                                    BigDecimal maxAmount,
                                                    LocalDateTime startDate,
                                                    LocalDateTime endDate,
                                                    Pageable pageable) {
        Specification<Payment> spec = Specification
                .where(PaymentSpecification.orderIdEquals(orderId))
                .and(PaymentSpecification.statusEquals(status))
                .and(PaymentSpecification.amountBetween(minAmount, maxAmount))
                .and(PaymentSpecification.createdAtBetween(startDate, endDate));

        return paymentRepository.findAll(spec, pageable).map(this::mapToResponse);
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