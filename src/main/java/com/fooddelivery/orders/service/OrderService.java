package com.fooddelivery.orders.service;

import com.fooddelivery.catalog.entity.MenuItem;
import com.fooddelivery.catalog.repository.MenuItemRepository;
import com.fooddelivery.orders.dto.CreateOrderRequest;
import com.fooddelivery.orders.dto.OrderItemDto;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        MenuItemRepository menuItemRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.objectMapper = objectMapper;
    }

    public Order createOrder(CreateOrderRequest request, UUID clientId) {

        BigDecimal subtotal = BigDecimal.ZERO;
        List<Map<String, Object>> snapshotItems = new ArrayList<>();

        for (OrderItemDto itemDto : request.getItems()) {

            MenuItem item = menuItemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new RuntimeException("Menu item not found"));

            BigDecimal itemTotal =
                    item.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));

            subtotal = subtotal.add(itemTotal);

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("itemId", item.getId());
            snapshot.put("name", item.getName());
            snapshot.put("price", item.getPrice());
            snapshot.put("quantity", itemDto.getQuantity());

            snapshotItems.add(snapshot);
        }

        String itemsJson;

        try {
            itemsJson = objectMapper.writeValueAsString(snapshotItems);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize order items");
        }

        Order order = new Order();

        order.setOrderNumber(generateOrderNumber());
        order.setClientId(clientId);
        order.setRestaurantId(request.getRestaurantId());
        order.setStatus(OrderStatus.PENDING);
        order.setItems(itemsJson);
        order.setSubtotal(subtotal);

        BigDecimal deliveryFee = BigDecimal.valueOf(100);

        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(subtotal.add(deliveryFee));
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    public List<Order> getClientOrders(UUID clientId) {
        return orderRepository.findByClientId(clientId);
    }

    public List<Order> getRestaurantOrders(UUID restaurantId) {
        return orderRepository.findByRestaurantId(restaurantId);
    }

    public Order cancelOrder(UUID orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);

        return orderRepository.save(order);
    }

    public Order confirmOrder(UUID orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be confirmed");
        }

        order.setStatus(OrderStatus.CONFIRMED);

        return orderRepository.save(order);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8);
    }
}