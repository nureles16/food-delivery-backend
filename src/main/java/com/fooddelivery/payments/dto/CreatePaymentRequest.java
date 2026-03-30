package com.fooddelivery.payments.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Запрос на создание нового платежа")
public class CreatePaymentRequest {

    @NotNull
    @Schema(description = "UUID заказа, к которому относится платеж", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID orderId;

    @NotNull
    @Schema(description = "UUID клиента, создающего платеж", example = "1c9d4f00-0c1b-4b7b-8f2d-3f1d5f1d1c1c", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID clientId;

    private UUID restaurantId;
    private BigDecimal deliveryFee;
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Schema(description = "Сумма платежа", example = "500.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @DecimalMin(value = "0.0")
    @Schema(description = "Комиссия платформы (если есть)", example = "50.50", required = false)
    private BigDecimal platformFee;

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee;}

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
}