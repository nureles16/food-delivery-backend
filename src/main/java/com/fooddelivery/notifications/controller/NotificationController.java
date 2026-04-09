package com.fooddelivery.notifications.controller;

import com.fooddelivery.notifications.dto.CreateNotificationRequest;
import com.fooddelivery.notifications.dto.NotificationResponse;
import com.fooddelivery.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Operation(summary = "Создать уведомление для себя")
    @PostMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<NotificationResponse> createMyNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.ok(notificationService.createNotification(request));
    }

    @Operation(summary = "Создать уведомление для указанного пользователя (только SUPER_ADMIN)")
    @PostMapping("/admin/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<NotificationResponse> createSystemNotification(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.ok(notificationService.createSystemNotification(
                userId, request.getTitle(), request.getMessage()));
    }

    @Operation(summary = "Получить свои уведомления с фильтрацией")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(notificationService.getMyNotifications(
                read, startDate, endDate, keyword, pageable));
    }

    @Operation(summary = "Получить уведомления любого пользователя (только SUPER_ADMIN)")
    @GetMapping("/admin/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<NotificationResponse>> getUserNotificationsByAdmin(
            @PathVariable UUID userId,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(notificationService.getUserNotificationsByAdmin(
                userId, read, startDate, endDate, keyword, pageable));
    }

    @Operation(summary = "Отметить своё уведомление как прочитанное")
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
}