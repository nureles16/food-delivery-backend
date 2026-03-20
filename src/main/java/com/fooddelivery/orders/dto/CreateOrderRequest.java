package com.fooddelivery.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request to create a new order")
public class CreateOrderRequest {

    @Schema(description = "Restaurant ID", example = "6c6d6b6b-9f1c-4b10-b0fa-123456789012", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "RestaurantId обязателен")
    private UUID restaurantId;

    @Schema(description = "Delivery address", example = "Bishkek, Kievskaya 120", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Адрес доставки обязателен")
    @Size(max = 255, message = "Адрес доставки слишком длинный")
    private String deliveryAddress;

    @Schema(description = "Items in the order", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Список товаров не может быть пустым")
    @Valid // 🔹 Чтобы валидация OrderItemDto сработала
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