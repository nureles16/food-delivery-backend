package com.fooddelivery.mapper;

import com.fooddelivery.orders.dto.OrderResponse;
import com.fooddelivery.orders.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) return null;
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setClientId(order.getClientId());
        response.setRestaurantId(order.getRestaurantId());
        response.setStatus(order.getStatus());
        response.setItems(order.getItems());
        response.setSubtotal(order.getSubtotal());
        response.setPlatformCommission(order.getPlatformCommission());
        response.setDeliveryFee(order.getDeliveryFee());
        response.setTotalAmount(order.getTotalAmount());
        response.setDeliveryAddress(order.getDeliveryAddress());
        response.setPaymentId(order.getPaymentId());
        response.setCreatedAt(order.getCreatedAt());
        response.setPaidAt(order.getPaidAt());
        response.setCafeConfirmedAt(order.getCafeConfirmedAt());
        response.setDeliveredAt(order.getDeliveredAt());
        response.setCancelledReason(order.getCancelledReason());
        response.setYandexDeliveryId(order.getYandexDeliveryId());
        response.setYandexTrackingUrl(order.getYandexTrackingUrl());
        response.setEstimatedDeliveryAt(order.getEstimatedDeliveryAt());
        return response;
    }
}