package com.fooddelivery.orders.repository;

import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime dateTime);
    List<Order> findByStatusAndPaidAtBefore(OrderStatus status, LocalDateTime dateTime);
    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    Long getNextOrderNumber();
}