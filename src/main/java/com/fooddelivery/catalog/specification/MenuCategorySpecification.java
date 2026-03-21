package com.fooddelivery.catalog.specification;

import com.fooddelivery.catalog.entity.MenuCategory;
import com.fooddelivery.catalog.entity.Restaurant;
import org.springframework.data.jpa.domain.Specification;
import java.util.UUID;

public class MenuCategorySpecification {

    public static Specification<MenuCategory> restaurantIdEquals(UUID restaurantId) {
        return (root, query, cb) ->
                restaurantId == null ? cb.conjunction() :
                        cb.equal(root.get("restaurant").get("id"), restaurantId);
    }

    public static Specification<MenuCategory> nameContains(String name) {
        return (root, query, cb) ->
                name == null ? cb.conjunction() :
                        cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<MenuCategory> positionBetween(Integer minPosition, Integer maxPosition) {
        return (root, query, cb) -> {
            if (minPosition == null && maxPosition == null) return cb.conjunction();
            if (minPosition != null && maxPosition != null)
                return cb.between(root.get("position"), minPosition, maxPosition);
            if (minPosition != null)
                return cb.greaterThanOrEqualTo(root.get("position"), minPosition);
            // maxPosition != null
            return cb.lessThanOrEqualTo(root.get("position"), maxPosition);
        };
    }
}