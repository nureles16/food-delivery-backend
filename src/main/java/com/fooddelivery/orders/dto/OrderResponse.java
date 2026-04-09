package com.fooddelivery.orders.dto;


import com.fooddelivery.orders.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private UUID clientId;
    private UUID restaurantId;
    private OrderStatus status;
    private String items;
    private BigDecimal subtotal;
    private BigDecimal platformCommission;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private Address deliveryAddress;
    private UUID paymentId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime cafeConfirmedAt;
    private LocalDateTime deliveredAt;
    private String cancelledReason;
    private String yandexDeliveryId;
    private String yandexTrackingUrl;
    private LocalDateTime estimatedDeliveryAt;

    public void setId(UUID id) {
        this.id = id;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public void setPlatformCommission(BigDecimal platformCommission) {
        this.platformCommission = platformCommission;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setDeliveryAddress(Address deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public void setCafeConfirmedAt(LocalDateTime cafeConfirmedAt) {
        this.cafeConfirmedAt = cafeConfirmedAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public void setCancelledReason(String cancelledReason) {
        this.cancelledReason = cancelledReason;
    }

    public void setYandexDeliveryId(String yandexDeliveryId) {
        this.yandexDeliveryId = yandexDeliveryId;
    }

    public void setYandexTrackingUrl(String yandexTrackingUrl) {
        this.yandexTrackingUrl = yandexTrackingUrl;
    }

    public void setEstimatedDeliveryAt(LocalDateTime estimatedDeliveryAt) {
        this.estimatedDeliveryAt = estimatedDeliveryAt;
    }
}