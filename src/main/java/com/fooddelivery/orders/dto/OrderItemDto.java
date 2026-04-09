package com.fooddelivery.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Item inside order")
public class OrderItemDto {
    @NotNull(message = "ItemId обязателен")
    private UUID itemId;

    @NotNull(message = "Количество обязательно")
    @Min(value = 1, message = "Количество должно быть хотя бы 1")
    private Integer quantity;

    public UUID getItemId() { return itemId; }
    public void setItemId(UUID itemId) { this.itemId = itemId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}