package com.fooddelivery.payments.controller;

import com.fooddelivery.payments.dto.CreatePaymentRequest;
import com.fooddelivery.payments.dto.PaymentResponse;
import com.fooddelivery.payments.entity.PaymentStatus;
import com.fooddelivery.payments.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @Operation(summary = "Получить все платежи пользователя")
    @GetMapping("/client/{clientId}")
    public List<PaymentResponse> getPaymentsByClient(@PathVariable UUID clientId) {
        return paymentService.getPaymentsByClient(clientId);
    }

    @Operation(summary = "Получить все платежи по заказу")
    @GetMapping("/order/{orderId}")
    public List<PaymentResponse> getPaymentsByOrder(@PathVariable UUID orderId) {
        return paymentService.getPaymentsByOrder(orderId);
    }
}