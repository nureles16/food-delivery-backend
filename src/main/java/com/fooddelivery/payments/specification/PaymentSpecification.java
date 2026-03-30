package com.fooddelivery.payments.specification;

import com.fooddelivery.payments.entity.Payment;
import com.fooddelivery.payments.entity.PaymentStatus;
import com.fooddelivery.payments.entity.PayoutStatus;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentSpecification {

    public static Specification<Payment> clientIdEquals(UUID clientId) {
        return (root, query, cb) ->
                clientId == null ? cb.conjunction() : cb.equal(root.get("clientId"), clientId);
    }

    public static Specification<Payment> payoutStatusEquals(PayoutStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("payoutStatus"), status);
    }
    public static Specification<Payment> orderIdEquals(UUID orderId) {
        return (root, query, cb) ->
                orderId == null ? cb.conjunction() : cb.equal(root.get("orderId"), orderId);
    }

    public static Specification<Payment> statusEquals(PaymentStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Payment> amountBetween(BigDecimal minAmount, BigDecimal maxAmount) {
        return (root, query, cb) -> {
            if (minAmount == null && maxAmount == null) return cb.conjunction();
            if (minAmount != null && maxAmount != null)
                return cb.between(root.get("amount"), minAmount, maxAmount);
            if (minAmount != null)
                return cb.greaterThanOrEqualTo(root.get("amount"), minAmount);
            return cb.lessThanOrEqualTo(root.get("amount"), maxAmount);
        };
    }

    public static Specification<Payment> createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) return cb.conjunction();
            if (startDate != null && endDate != null)
                return cb.between(root.get("createdAt"), startDate, endDate);
            if (startDate != null)
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
        };
    }

    public static Specification<Payment> restaurantIdEquals(UUID restaurantId) {
        return (root, query, cb) -> restaurantId == null ? cb.conjunction() : cb.equal(root.get("restaurantId"), restaurantId);
    }
}