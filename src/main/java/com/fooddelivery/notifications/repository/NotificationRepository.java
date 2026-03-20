package com.fooddelivery.notifications.repository;

import com.fooddelivery.notifications.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findAllByUserId(UUID userId, Pageable pageable);
}