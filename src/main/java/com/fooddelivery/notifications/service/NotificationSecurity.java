package com.fooddelivery.notifications.service;

import com.fooddelivery.auth.security.CustomUserDetails;
import com.fooddelivery.notifications.repository.NotificationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("notificationSecurity")
public class NotificationSecurity {

    private final NotificationRepository notificationRepository;

    public NotificationSecurity(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Проверяет, может ли текущий пользователь получить доступ к уведомлению.
     * Суперадмин имеет доступ ко всем.
     */
    public boolean canAccessNotification(Authentication authentication, UUID notificationId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        boolean isSuperAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (isSuperAdmin) return true;

        return notificationRepository.existsByIdAndUserId(notificationId, user.getId());
    }

    /**
     * Проверяет, что пользователь аутентифицирован (для эндпоинтов, где требуется только аутентификация).
     */
    public boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }
}