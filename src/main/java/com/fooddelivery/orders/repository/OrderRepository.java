package com.fooddelivery.orders.repository;

import com.fooddelivery.orders.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByClientId(UUID clientId, Pageable pageable);

    Page<Order> findByRestaurantId(UUID restaurantId, Pageable pageable);

}