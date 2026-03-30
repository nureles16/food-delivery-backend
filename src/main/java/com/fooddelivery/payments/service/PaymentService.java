package com.fooddelivery.payments.service;

import com.fooddelivery.payments.dto.CreatePaymentRequest;
import com.fooddelivery.payments.dto.PaymentResponse;
import com.fooddelivery.payments.entity.Payment;
import com.fooddelivery.payments.entity.PaymentStatus;
import com.fooddelivery.payments.entity.PayoutStatus;
import com.fooddelivery.payments.repository.PaymentRepository;
import com.fooddelivery.payments.specification.PaymentSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse createPayment(CreatePaymentRequest request) {
        if (paymentRepository.existsByOrderId(request.getOrderId())) {
            throw new RuntimeException("Payment for order " + request.getOrderId() + " already exists");
        }
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setClientId(request.getClientId());
        payment.setRestaurantId(request.getRestaurantId());
        payment.setAmount(request.getAmount());
        payment.setDeliveryFee(request.getDeliveryFee());
        payment.setPlatformFee(request.getPlatformFee());
        payment.setRestaurantPayout(request.getAmount()
                .subtract(request.getDeliveryFee())
                .subtract(request.getPlatformFee()));
        payment.setProvider("FREEDOM_PAY");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setPayoutStatus(PayoutStatus.PENDING);

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
    @Transactional
    public PaymentResponse refund(UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException(String.valueOf(paymentId)));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Refund allowed only for COMPLETED payments");
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Payment already refunded");
        }


        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());

        Payment updated = paymentRepository.save(payment);
        return mapToResponse(updated);
    }


    public Page<PaymentResponse> getPayouts(PayoutStatus status, Pageable pageable) {
        Specification<Payment> spec = Specification.where(PaymentSpecification.payoutStatusEquals(status));
        return paymentRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional
    public PaymentResponse markPayoutAsPaid(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException(String.valueOf(paymentId)));

        if (payment.getPayoutStatus() != PayoutStatus.PENDING) {
            throw new IllegalStateException("Payout already processed");
        }

        payment.setPayoutStatus(PayoutStatus.PAID_OUT);
        payment.setPayoutAt(LocalDateTime.now());

        Payment updated = paymentRepository.save(payment);
        return mapToResponse(updated);
    }

    public Page<PaymentResponse> getCafePayments(UUID restaurantId,
                                                 PaymentStatus status,
                                                 LocalDateTime startDate,
                                                 LocalDateTime endDate,
                                                 Pageable pageable) {
        Specification<Payment> spec = Specification
                .where(PaymentSpecification.restaurantIdEquals(restaurantId))
                .and(PaymentSpecification.statusEquals(status))
                .and(PaymentSpecification.createdAtBetween(startDate, endDate));

        return paymentRepository.findAll(spec, pageable).map(this::mapToResponse);
    }
}