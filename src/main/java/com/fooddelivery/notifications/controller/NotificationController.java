package com.fooddelivery.notifications.controller;

import com.fooddelivery.auth.security.CustomUserDetails;
import com.fooddelivery.notifications.dto.CreateNotificationRequest;
import com.fooddelivery.notifications.dto.NotificationResponse;
import com.fooddelivery.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Управление уведомлениями пользователей")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "Создать новое уведомление",
            description = "Отправляет уведомление пользователю и возвращает DTO уведомления",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Уведомление создано успешно",
                            content = @Content(schema = @Schema(implementation = NotificationResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Некорректные данные"
                    )
            }
    )
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or #request.userId == authentication.principal.id")
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(notificationService.createNotification(request));
    }

    @Operation(summary = "Получить все уведомления пользователя с фильтрацией")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @PathVariable UUID userId,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(notificationService.getUserNotifications(
                userId, read, startDate, endDate, keyword, pageable));
    }

    @Operation(summary = "Отметить уведомление как прочитанное")
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUserId));
    }
}