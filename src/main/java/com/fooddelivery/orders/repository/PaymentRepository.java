package com.fooddelivery.orders.repository;

import com.fooddelivery.orders.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    List<Payment> findByClientId(UUID clientId);
}