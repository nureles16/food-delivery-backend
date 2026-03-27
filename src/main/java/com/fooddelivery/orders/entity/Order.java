package com.fooddelivery.orders.entity;

import com.fooddelivery.orders.dto.Address;
import com.fooddelivery.orders.specification.AddressJsonConverter;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(columnDefinition = "jsonb")
    private String items;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee")
    private BigDecimal deliveryFee;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    private BigDecimal platformCommission;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Convert(converter = AddressJsonConverter.class)
    @Column(columnDefinition = "jsonb")
    private Address deliveryAddress;

    @Column(name = "cafe_confirmed_at")
    private LocalDateTime cafeConfirmedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "yandex_delivery_id")
    private String yandexDeliveryId;

    @Column(name = "yandex_tracking_url")
    private String yandexTrackingUrl;

    @Column(name = "estimated_delivery_at")
    private LocalDateTime estimatedDeliveryAt;

    @Column(name = "cancelled_reason")
    private String cancelledReason;

    private LocalDateTime paidAt;

    public Order() {
    }

    public UUID getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getClientId() {
        return clientId;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getItems() {
        return items;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Address getDeliveryAddress() { return deliveryAddress; }


    public LocalDateTime getCreatedAt() {
        return createdAt;
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

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setDeliveryAddress(Address deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getPlatformCommission() {
        return platformCommission;
    }

    public void setPlatformCommission(BigDecimal platformCommission) {
        this.platformCommission = platformCommission;
    }

    public LocalDateTime getCafeConfirmedAt() {
        return cafeConfirmedAt;
    }

    public void setCafeConfirmedAt(LocalDateTime cafeConfirmedAt) {
        this.cafeConfirmedAt = cafeConfirmedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public String getYandexDeliveryId() {
        return yandexDeliveryId;
    }

    public void setYandexDeliveryId(String yandexDeliveryId) {
        this.yandexDeliveryId = yandexDeliveryId;
    }

    public String getYandexTrackingUrl() {
        return yandexTrackingUrl;
    }

    public void setYandexTrackingUrl(String yandexTrackingUrl) {
        this.yandexTrackingUrl = yandexTrackingUrl;
    }

    public LocalDateTime getEstimatedDeliveryAt() {
        return estimatedDeliveryAt;
    }

    public void setEstimatedDeliveryAt(LocalDateTime estimatedDeliveryAt) {
        this.estimatedDeliveryAt = estimatedDeliveryAt;
    }

    public String getCancelledReason() {
        return cancelledReason;
    }

    public void setCancelledReason(String cancelledReason) {
        this.cancelledReason = cancelledReason;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}