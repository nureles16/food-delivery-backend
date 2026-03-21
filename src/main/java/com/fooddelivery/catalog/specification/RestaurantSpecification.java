package com.fooddelivery.catalog.specification;


import com.fooddelivery.catalog.entity.Restaurant;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class RestaurantSpecification {

    public static Specification<Restaurant> nameContains(String name) {
        return (root, query, cb) ->
                name == null ? cb.conjunction() :
                        cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Restaurant> cityEquals(String city) {
        return (root, query, cb) ->
                city == null ? cb.conjunction() :
                        cb.equal(root.get("city"), city);
    }

    public static Specification<Restaurant> cuisineTypeEquals(String cuisineType) {
        return (root, query, cb) ->
                cuisineType == null ? cb.conjunction() :
                        cb.equal(root.get("cuisineType"), cuisineType);
    }

    public static Specification<Restaurant> minOrderAmountGreaterThanOrEqual(BigDecimal minOrderAmount) {
        return (root, query, cb) ->
                minOrderAmount == null ? cb.conjunction() :
                        cb.greaterThanOrEqualTo(root.get("minOrderAmount"), minOrderAmount);
    }

    // Active flag (if the entity has such a field)
    public static Specification<Restaurant> isActive(Boolean active) {
        return (root, query, cb) ->
                active == null ? cb.conjunction() :
                        cb.equal(root.get("active"), active);
    }
}