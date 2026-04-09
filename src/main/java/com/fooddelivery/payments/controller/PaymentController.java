package com.fooddelivery.payments.controller;

import com.fooddelivery.auth.security.CustomUserDetails;
import com.fooddelivery.exceptions.BadRequestException;
import com.fooddelivery.payments.dto.*;
import com.fooddelivery.payments.entity.PaymentStatus;
import com.fooddelivery.payments.entity.PayoutStatus;
import com.fooddelivery.payments.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Управление платежами")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }


    @Operation(summary = "Инициировать платёж для заказа (internal)")
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('SYSTEM') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<InitiatePaymentResponse> initiatePayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {

        validateInternalToken(internalToken);
        log.debug("Initiate payment request for orderId={}", request.getOrderId());
        InitiatePaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Webhook от Freedom Pay (публичный, без авторизации)")
    @PostMapping("/webhook/freedompay")
    public ResponseEntity<Void> handleFreedomPayWebhook(
            @RequestBody WebhookPayload payload,
            @RequestHeader(value = "X-Signature", required = false) String signature) {
        log.info("Received webhook from Freedom Pay for orderId={}", payload.getOrderId());
        if (signature == null || signature.isBlank()) {
            log.error("Missing X-Signature header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        paymentService.processWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Инициировать возврат платежа (только суперадмин)")
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundRequest refundRequest) {
        PaymentResponse response = paymentService.refund(paymentId, refundRequest.getCancelledReason());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Автоматический возврат по отмене заказа (internal)")
    @PostMapping("/internal/refund-by-order/{orderId}")
    @PreAuthorize("hasRole('SYSTEM') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<PaymentResponse> refundByOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody RefundRequest refundRequest,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {

        validateInternalToken(internalToken);
        log.info("Auto-refund requested by OrderService for orderId={}, reason={}", orderId, refundRequest.getCancelledReason());
        PaymentResponse response = paymentService.refundByOrderId(orderId, refundRequest.getCancelledReason());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Получить список выплат ресторанам (только суперадмин)")
    @GetMapping("/admin/payouts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getPayouts(
            @RequestParam(required = false) PayoutStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(paymentService.getPayouts(status, pageable));
    }

    @Operation(summary = "Отметить выплату как выполненную (только суперадмин)")
    @PatchMapping("/admin/payouts/{paymentId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PaymentResponse> markPayoutAsPaid(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.markPayoutAsPaid(paymentId));
    }


    @Operation(summary = "Получить свои платежи с фильтрацией")
    @GetMapping("/my")
    @PreAuthorize("@paymentSecurity.canAccessMyPayments(authentication)")
    public ResponseEntity<Page<PaymentResponse>> getMyPayments(
            Authentication authentication,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        validateDateRange(startDate, endDate);
        validateAmountRange(minAmount, maxAmount);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(paymentService.getMyPayments(
                orderId, status, minAmount, maxAmount, startDate, endDate, pageable));
    }

    @Operation(summary = "Финансовая сводка для кафе-админа")
    @GetMapping("/cafe/payments")
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getCafePayments(
            Authentication authentication,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        validateDateRange(startDate, endDate);

        UUID restaurantId = extractRestaurantIdFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(paymentService.getCafePayments(
                restaurantId, status, startDate, endDate, pageable));
    }

    @Operation(summary = "Получить детали платежа по ID")
    @GetMapping("/{paymentId}")
    @PreAuthorize("@paymentSecurity.canAccessPayment(authentication, #paymentId)")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable UUID paymentId,
            Authentication authentication) {
        PaymentResponse response = paymentService.getPaymentById(paymentId, authentication);
        return ResponseEntity.ok(response);
    }


    private void validateInternalToken(String token) {
        final String EXPECTED_TOKEN = "your-secure-internal-token"; // вынести в конфигурацию
        if (token == null || !EXPECTED_TOKEN.equals(token)) {
            throw new AccessDeniedException("Invalid or missing internal token");
        }
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }
    }

    private void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BadRequestException("minAmount cannot be greater than maxAmount");
        }
    }

    private UUID extractRestaurantIdFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            UUID cafeId = userDetails.getCafeId();
            if (cafeId == null) {
                throw new BadRequestException("User is not associated with any cafe");
            }
            return cafeId;
        }
        throw new BadRequestException("Invalid authentication principal");
    }
}