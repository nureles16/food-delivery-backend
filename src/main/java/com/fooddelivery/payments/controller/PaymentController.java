package com.fooddelivery.payments.controller;

import com.fooddelivery.auth.security.CustomUserDetails;
import com.fooddelivery.payments.dto.CreatePaymentRequest;
import com.fooddelivery.payments.dto.PaymentResponse;
import com.fooddelivery.payments.entity.PaymentStatus;
import com.fooddelivery.payments.entity.PayoutStatus;
import com.fooddelivery.payments.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Создать новый платеж")
    @PostMapping
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @Operation(summary = "Обновить статус платежа")
    @PatchMapping("/{paymentId}/status")
    public PaymentResponse updateStatus(@PathVariable UUID paymentId,
                                        @RequestParam PaymentStatus status) {
        return paymentService.updateStatus(paymentId, status);
    }

    @Operation(summary = "Получить все платежи пользователя с фильтрацией")
    @GetMapping("/my")
    public ResponseEntity<Page<PaymentResponse>> getMyPayments(
            Authentication authentication,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID clientId = UUID.fromString(authentication.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ResponseEntity.ok(paymentService.getPaymentsByClient(
                clientId, orderId, status, minAmount, maxAmount, startDate, endDate, pageable));
    }

    @Operation(summary = "Получить все платежи по заказу с фильтрацией")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByOrder(
            @PathVariable UUID orderId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ResponseEntity.ok(paymentService.getPaymentsByOrder(
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
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID restaurantId = extractRestaurantIdFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(paymentService.getCafePayments(
                restaurantId, status, startDate, endDate, pageable));
    }

    private UUID extractRestaurantIdFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            UUID cafeId = userDetails.getCafeId();
            if (cafeId == null) {
                throw new AccessDeniedException("Пользователь не привязан к кафе");
            }
            return cafeId;
        }
        throw new AccessDeniedException("Неверный объект аутентификации");
    }

    @Operation(summary = "Получить список выплат ресторанам (только суперадмин)")
    @GetMapping("/admin/payouts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getPayouts(
            @RequestParam(required = false) PayoutStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(paymentService.getPayouts(status, pageable));
    }

    @Operation(summary = "Отметить выплату как выполненную (только суперадмин)")
    @PatchMapping("/admin/payouts/{paymentId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PaymentResponse> markPayoutAsPaid(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.markPayoutAsPaid(paymentId));
    }

    @Operation(summary = "Инициировать возврат платежа (только суперадмин)")
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable UUID paymentId,
            @RequestParam String reason
    ) {
        return ResponseEntity.ok(paymentService.refund(paymentId, reason));
    }
}