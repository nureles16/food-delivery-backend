package com.fooddelivery.catalog.specification;

import com.fooddelivery.catalog.entity.MenuItem;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.util.UUID;

public class MenuItemSpecification {

    public static Specification<MenuItem> categoryIdEquals(UUID categoryId) {
        return (root, query, cb) ->
                categoryId == null ? cb.conjunction() : cb.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<MenuItem> nameContains(String name) {
        return (root, query, cb) ->
                name == null ? cb.conjunction() :
                        cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<MenuItem> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return cb.conjunction();
            if (minPrice != null && maxPrice != null)
                return cb.between(root.get("price"), minPrice, maxPrice);
            if (minPrice != null)
                return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    public static Specification<MenuItem> availableEquals(Boolean available) {
        return (root, query, cb) ->
                available == null ? cb.conjunction() : cb.equal(root.get("available"), available);
    }
}