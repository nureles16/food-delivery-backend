package com.fooddelivery.orders.specification;

import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class OrderSpecification {

    public static Specification<Order> clientIdEquals(UUID clientId) {
        return (root, query, cb) ->
                clientId == null ? cb.conjunction() : cb.equal(root.get("clientId"), clientId);
    }

    public static Specification<Order> restaurantIdEquals(UUID restaurantId) {
        return (root, query, cb) ->
                restaurantId == null ? cb.conjunction() : cb.equal(root.get("restaurantId"), restaurantId);
    }

    public static Specification<Order> statusEquals(OrderStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> orderNumberContains(String orderNumber) {
        return (root, query, cb) ->
                orderNumber == null || orderNumber.isEmpty() ? cb.conjunction() :
                        cb.like(cb.lower(root.get("orderNumber")), "%" + orderNumber.toLowerCase() + "%");
    }

    public static Specification<Order> createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) return cb.conjunction();
            if (startDate != null && endDate != null)
                return cb.between(root.get("createdAt"), startDate, endDate);
            if (startDate != null)
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            // endDate != null
            return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
        };
    }

    public static Specification<Order> totalAmountBetween(BigDecimal minAmount, BigDecimal maxAmount) {
        return (root, query, cb) -> {
            if (minAmount == null && maxAmount == null) return cb.conjunction();
            if (minAmount != null && maxAmount != null)
                return cb.between(root.get("totalAmount"), minAmount, maxAmount);
            if (minAmount != null)
                return cb.greaterThanOrEqualTo(root.get("totalAmount"), minAmount);
            // maxAmount != null
            return cb.lessThanOrEqualTo(root.get("totalAmount"), maxAmount);
        };
    }
}