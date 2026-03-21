package com.fooddelivery.catalog.service;

import com.fooddelivery.catalog.dto.CreateCategoryRequest;
import com.fooddelivery.catalog.entity.MenuCategory;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.repository.MenuCategoryRepository;
import com.fooddelivery.catalog.repository.RestaurantRepository;
import com.fooddelivery.catalog.specification.MenuCategorySpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MenuCategoryService {

    private final MenuCategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;

    public MenuCategoryService(MenuCategoryRepository categoryRepository,
                               RestaurantRepository restaurantRepository) {
        this.categoryRepository = categoryRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public MenuCategory createCategory(CreateCategoryRequest request) {

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        MenuCategory category = new MenuCategory();
        category.setName(request.getName());
        category.setPosition(request.getPosition());
        category.setRestaurant(restaurant);

        return categoryRepository.save(category);
    }

    public MenuCategory updateCategory(UUID id, CreateCategoryRequest request) {

        MenuCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setName(request.getName());
        category.setPosition(request.getPosition());

        return categoryRepository.save(category);
    }

    public void deleteCategory(UUID id) {

        MenuCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        categoryRepository.delete(category);
    }

    public Page<MenuCategory> getCategoriesByRestaurant(UUID restaurantId,
                                                        String name,
                                                        Integer minPosition,
                                                        Integer maxPosition,
                                                        Pageable pageable) {
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        Specification<MenuCategory> spec = Specification
                .where(MenuCategorySpecification.restaurantIdEquals(restaurantId))
                .and(MenuCategorySpecification.nameContains(name))
                .and(MenuCategorySpecification.positionBetween(minPosition, maxPosition));

        return categoryRepository.findAll(spec, pageable);
    }
}