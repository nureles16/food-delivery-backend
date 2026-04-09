package com.fooddelivery.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request to create a new order")
public class CreateOrderRequest {
    @NotNull(message = "RestaurantId обязателен")
    private UUID restaurantId;

    @NotNull(message = "Delivery address is required")
    @Valid
    private Address deliveryAddress;

    @NotEmpty(message = "Список товаров не может быть пустым")
    @Valid
    private List<OrderItemDto> items;

    public UUID getRestaurantId() { return restaurantId; }
    public void setRestaurantId(UUID restaurantId) { this.restaurantId = restaurantId; }
    public Address getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(Address deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }
}