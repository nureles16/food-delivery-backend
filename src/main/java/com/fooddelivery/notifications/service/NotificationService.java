package com.fooddelivery.notifications.service;

import com.fooddelivery.notifications.dto.CreateNotificationRequest;
import com.fooddelivery.notifications.dto.NotificationResponse;
import com.fooddelivery.notifications.entity.Notification;
import com.fooddelivery.notifications.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Создать уведомление
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        return mapToResponse(saved);
    }

    // Получить уведомления пользователя
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {

        return notificationRepository.findAllByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    // Отметить уведомление как прочитанное
    public NotificationResponse markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setRead(true);
        notification.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(notificationRepository.save(notification));
    }

    // Преобразование сущности в DTO
    private NotificationResponse mapToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setUserId(notification.getUserId());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setRead(notification.isRead());
        response.setCreatedAt(notification.getCreatedAt());
        response.setUpdatedAt(notification.getUpdatedAt());
        return response;
    }
}