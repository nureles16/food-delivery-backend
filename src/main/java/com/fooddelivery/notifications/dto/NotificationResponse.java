package com.fooddelivery.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "DTO для ответа уведомления")
public class NotificationResponse {

    @Schema(description = "ID уведомления", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "ID пользователя", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;

    @Schema(description = "Заголовок уведомления", example = "Новый заказ")
    private String title;

    @Schema(description = "Текст уведомления", example = "Ваш заказ #123 готов к доставке")
    private String message;

    @Schema(description = "Статус прочтения уведомления", example = "false")
    private boolean isRead;

    @Schema(description = "Дата создания уведомления", example = "2026-03-07T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Дата последнего обновления уведомления", example = "2026-03-07T12:05:00")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}