package com.fooddelivery.catalog.specification;


import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.entity.WorkingHours;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

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

    public static Specification<Restaurant> isOpenNow() {
        return (root, query, cb) -> {
            // Подзапрос для поиска интервалов работы, покрывающих текущее время
            LocalTime now = LocalTime.now();
            DayOfWeek today = LocalDate.now().getDayOfWeek();

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<WorkingHours> whRoot = subquery.from(WorkingHours.class);
            subquery.select(cb.literal(1L));
            subquery.where(
                    cb.equal(whRoot.get("restaurant"), root),
                    cb.equal(whRoot.get("dayOfWeek"), today),
                    cb.lessThanOrEqualTo(whRoot.get("openTime"), now),
                    cb.greaterThanOrEqualTo(whRoot.get("closeTime"), now)
            );
            return cb.exists(subquery);
        };
    }
}