package com.fooddelivery.notifications.service;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.service.AuthService;
import com.fooddelivery.exceptions.ForbiddenException;
import com.fooddelivery.exceptions.NotFoundException;
import com.fooddelivery.mapper.NotificationMapper;
import com.fooddelivery.notifications.dto.CreateNotificationRequest;
import com.fooddelivery.notifications.dto.NotificationResponse;
import com.fooddelivery.notifications.entity.Notification;
import com.fooddelivery.notifications.repository.NotificationRepository;
import com.fooddelivery.notifications.specification.NotificationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthService authService;
    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               AuthService authService,
                               NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.authService = authService;
        this.notificationMapper = notificationMapper;
    }

    private User getCurrentUser() {
        return authService.getCurrentUser();
    }

    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        User currentUser = getCurrentUser();
        Notification notification = new Notification();
        notification.setUserId(currentUser.getId());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toResponse(saved);
    }

    @Transactional
    public NotificationResponse createSystemNotification(UUID targetUserId, String title, String message) {
        Notification notification = new Notification();
        notification.setUserId(targetUserId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Boolean read,
                                                         LocalDateTime startDate,
                                                         LocalDateTime endDate,
                                                         String keyword,
                                                         Pageable pageable) {
        User currentUser = getCurrentUser();
        UUID currentUserId = currentUser.getId();

        Specification<Notification> spec = Specification
                .where(NotificationSpecification.userIdEquals(currentUserId))
                .and(NotificationSpecification.readEquals(read))
                .and(NotificationSpecification.createdAtBetween(startDate, endDate))
                .and(NotificationSpecification.titleOrMessageContains(keyword));

        return notificationRepository.findAll(spec, pageable)
                .map(notificationMapper::toResponse);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        User currentUser = getCurrentUser();
        UUID currentUserId = currentUser.getId();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));

        if (!notification.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("Cannot mark another user's notification as read");
        }

        notification.setRead(true);
        notification.setUpdatedAt(LocalDateTime.now());
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotificationsByAdmin(UUID userId,
                                                                  Boolean read,
                                                                  LocalDateTime startDate,
                                                                  LocalDateTime endDate,
                                                                  String keyword,
                                                                  Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Only super admin can view another user's notifications");
        }

        Specification<Notification> spec = Specification
                .where(NotificationSpecification.userIdEquals(userId))
                .and(NotificationSpecification.readEquals(read))
                .and(NotificationSpecification.createdAtBetween(startDate, endDate))
                .and(NotificationSpecification.titleOrMessageContains(keyword));

        return notificationRepository.findAll(spec, pageable)
                .map(notificationMapper::toResponse);
    }
}