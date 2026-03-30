package com.fooddelivery.payments.dto;

import com.fooddelivery.payments.entity.PaymentStatus;
import com.fooddelivery.payments.entity.PayoutStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ответ с информацией о платеже")
public class PaymentResponse {
    private UUID restaurantId;
    private BigDecimal deliveryFee;
    private BigDecimal restaurantPayout;
    private PayoutStatus payoutStatus;
    private String provider;
    private String providerPaymentId;
    @Schema(description = "UUID платежа", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "UUID заказа, к которому относится платеж", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID orderId;

    @Schema(description = "UUID клиента", example = "1c9d4f00-0c1b-4b7b-8f2d-3f1d5f1d1c1c")
    private UUID clientId;

    @Schema(description = "Сумма платежа", example = "500.50")
    private BigDecimal amount;

    @Schema(description = "Комиссия платформы", example = "50.50")
    private BigDecimal platformFee;

    @Schema(description = "Статус платежа", example = "PENDING")
    private PaymentStatus status;

    @Schema(description = "Дата и время создания платежа", example = "2026-03-07T12:34:56")
    private LocalDateTime createdAt;

    @Schema(description = "Дата и время последнего обновления платежа", example = "2026-03-07T12:35:00")
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getRestaurantPayout() {
        return restaurantPayout;
    }

    public void setRestaurantPayout(BigDecimal restaurantPayout) {
        this.restaurantPayout = restaurantPayout;
    }

    public PayoutStatus getPayoutStatus() {
        return payoutStatus;
    }

    public void setPayoutStatus(PayoutStatus payoutStatus) {
        this.payoutStatus = payoutStatus;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}