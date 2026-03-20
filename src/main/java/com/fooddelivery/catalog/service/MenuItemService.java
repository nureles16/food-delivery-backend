package com.fooddelivery.catalog.service;

import com.fooddelivery.catalog.dto.MenuItemDto;
import com.fooddelivery.catalog.entity.MenuItem;
import com.fooddelivery.catalog.repository.MenuItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;

    public MenuItemService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    public MenuItem create(MenuItemDto dto) {
        MenuItem item = new MenuItem();
        item.setCategoryId(dto.getCategoryId());
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setImageUrl(dto.getImageUrl());
        item.setAvailable(dto.isAvailable());
        return menuItemRepository.save(item);
    }

    public MenuItem update(UUID id, MenuItemDto dto) {
        Optional<MenuItem> optional = menuItemRepository.findById(id);
        if (!optional.isPresent()) {
            throw new RuntimeException("MenuItem not found");
        }
        MenuItem item = optional.get();
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setImageUrl(dto.getImageUrl());
        item.setAvailable(dto.isAvailable());
        return menuItemRepository.save(item);
    }

    public MenuItem updateAvailability(UUID id, boolean isAvailable) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MenuItem not found"));
        item.setAvailable(isAvailable);
        return menuItemRepository.save(item);
    }

    public Page<MenuItem> getByCategory(UUID categoryId, Pageable pageable) {
        return menuItemRepository.findAllByCategoryId(categoryId, pageable);
    }
}