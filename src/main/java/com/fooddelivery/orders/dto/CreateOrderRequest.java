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

    @NotBlank(message = "Адрес доставки обязателен")
    @Size(max = 255, message = "Адрес доставки слишком длинный")
    @Schema(description = "Delivery address", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private Address deliveryAddress;

    @Schema(description = "Items in the order", requiredMode = Schema.RequiredMode.REQUIRED)
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