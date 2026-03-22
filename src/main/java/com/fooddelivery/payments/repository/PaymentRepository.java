package com.fooddelivery.payments.repository;

import com.fooddelivery.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {
    Page<Payment> findByClientId(UUID clientId, Pageable pageable);

    Page<Payment> findByOrderId(UUID orderId, Pageable pageable);
}