package com.fooddelivery.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request to create a new order")
public class CreateOrderRequest {

    @Schema(description = "Restaurant ID", example = "6c6d6b6b-9f1c-4b10-b0fa-123456789012")
    @NotNull
    private UUID restaurantId;

    @Schema(description = "Delivery address", example = "Bishkek, Kievskaya 120")
    @NotNull
    private String deliveryAddress;

    @Schema(description = "Items in the order")
    @NotEmpty
    private List<OrderItemDto> items;

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
}