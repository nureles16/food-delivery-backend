package com.fooddelivery.notifications.service;

import com.fooddelivery.exceptions.NotFoundException;
import com.fooddelivery.notifications.dto.CreateNotificationRequest;
import com.fooddelivery.notifications.dto.NotificationResponse;
import com.fooddelivery.notifications.entity.Notification;
import com.fooddelivery.notifications.repository.NotificationRepository;
import com.fooddelivery.notifications.specification.NotificationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

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

    public Page<NotificationResponse> getUserNotifications(UUID userId,
                                                           Boolean read,
                                                           LocalDateTime startDate,
                                                           LocalDateTime endDate,
                                                           String keyword,
                                                           Pageable pageable) {

        Specification<Notification> spec = Specification
                .where(NotificationSpecification.userIdEquals(userId))
                .and(NotificationSpecification.readEquals(read))
                .and(NotificationSpecification.createdAtBetween(startDate, endDate))
                .and(NotificationSpecification.titleOrMessageContains(keyword));

        return notificationRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    public NotificationResponse markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        notification.setRead(true);
        notification.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(notificationRepository.save(notification));
    }

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