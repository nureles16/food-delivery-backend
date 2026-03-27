package com.fooddelivery.catalog.service;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.service.AuthService;
import com.fooddelivery.catalog.dto.CreateCategoryRequest;
import com.fooddelivery.catalog.entity.MenuCategory;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.repository.MenuCategoryRepository;
import com.fooddelivery.catalog.repository.MenuItemRepository;
import com.fooddelivery.catalog.repository.RestaurantRepository;
import com.fooddelivery.catalog.specification.MenuCategorySpecification;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MenuCategoryService {

    private static final Logger log = LoggerFactory.getLogger(MenuCategoryService.class);

    private final MenuCategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final AuthService authService;

    public MenuCategoryService(MenuCategoryRepository categoryRepository,
                               RestaurantRepository restaurantRepository,
                               MenuItemRepository menuItemRepository,
                               AuthService authService) {
        this.categoryRepository = categoryRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.authService = authService;
    }

    @Transactional
    public MenuCategory createCategory(CreateCategoryRequest request) {
        User currentUser = authService.getCurrentUser();

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found: " + request.getRestaurantId()));
        checkRestaurantAccess(currentUser, restaurant.getId());

        MenuCategory category = new MenuCategory();
        category.setName(request.getName());
        category.setPosition(request.getPosition());
        category.setRestaurant(restaurant);

        MenuCategory saved = categoryRepository.save(category);
        log.info("Category created: {} for restaurant {} by user {}", saved.getId(), restaurant.getId(), currentUser.getId());
        return saved;
    }

    @Transactional
    public MenuCategory updateCategory(UUID id, CreateCategoryRequest request) {
        User currentUser = authService.getCurrentUser();

        MenuCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));

        UUID restaurant = category.getRestaurant();
        if (restaurant == null) {
            throw new EntityNotFoundException("Restaurant not found for category " + id);
        }
        checkRestaurantAccess(currentUser, restaurant);

        if (request.getRestaurantId() != null && !request.getRestaurantId().equals(restaurant)) {
            Restaurant newRestaurant = restaurantRepository.findById(request.getRestaurantId())
                    .orElseThrow(() -> new EntityNotFoundException("Restaurant not found: " + request.getRestaurantId()));
            checkRestaurantAccess(currentUser, newRestaurant.getId());
            category.setRestaurant(newRestaurant);
        }

        category.setName(request.getName());
        category.setPosition(request.getPosition());

        MenuCategory updated = categoryRepository.save(category);
        log.info("Category updated: {} by user {}", updated.getId(), currentUser.getId());
        return updated;
    }

    @Transactional
    public void deleteCategory(UUID id) {
        User currentUser = authService.getCurrentUser();

        MenuCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));

        UUID restaurant = category.getRestaurant();
        if (restaurant == null) {
            throw new EntityNotFoundException("Restaurant not found for category " + id);
        }
        checkRestaurantAccess(currentUser, restaurant);

        boolean hasItems = menuItemRepository.existsByCategoryId(id);
        if (hasItems) {
            log.warn("Attempt to delete non-empty category {} by user {}", id, currentUser.getId());
            throw new IllegalStateException("Cannot delete category that contains menu items");
        }

        categoryRepository.delete(category);
        log.info("Category deleted: {} by user {}", id, currentUser.getId());
    }

    public Page<MenuCategory> getCategoriesByRestaurant(UUID restaurantId,
                                                        String name,
                                                        Integer minPosition,
                                                        Integer maxPosition,
                                                        Pageable pageable) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new EntityNotFoundException("Restaurant not found: " + restaurantId);
        }

        Specification<MenuCategory> spec = Specification
                .where(MenuCategorySpecification.restaurantIdEquals(restaurantId))
                .and(MenuCategorySpecification.nameContains(name))
                .and(MenuCategorySpecification.positionBetween(minPosition, maxPosition));

        return categoryRepository.findAll(spec, pageable);
    }

    private void checkRestaurantAccess(User user, UUID restaurantId) {
        if (user.getRole() == Role.SUPER_ADMIN) {
            return;
        }
        if (user.getRole() == Role.CAFE_ADMIN) {
            if (user.getCafeId() == null) {
                log.warn("Access denied: CAFE_ADMIN user {} has no associated cafe", user.getId());
                throw new AccessDeniedException("You are not assigned to any restaurant");
            }
            if (!user.getCafeId().equals(restaurantId)) {
                log.warn("Access denied: user {} tried to access restaurant {} but belongs to {}",
                        user.getId(), restaurantId, user.getCafeId());
                throw new AccessDeniedException("You don't have permission to access this restaurant");
            }
            return;
        }
        log.warn("Access denied: user {} with role {} tried to access restaurant {}",
                user.getId(), user.getRole(), restaurantId);
        throw new AccessDeniedException("Only SUPER_ADMIN or CAFE_ADMIN can manage categories");
    }
}