package com.fooddelivery.orders.controller;

import com.fooddelivery.orders.dto.CreateOrderRequest;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "API для управления заказами")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Create order", description = "Client creates a new order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<Order> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {

        UUID clientId = UUID.fromString(authentication.getName());

        Order order = orderService.createOrder(request, clientId);

        return ResponseEntity.ok(order);
    }


    @Operation(summary = "Get my orders", description = "Client retrieves their order history")
    @ApiResponse(responseCode = "200", description = "Orders retrieved")
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {

        UUID clientId = UUID.fromString(authentication.getName());

        List<Order> orders = orderService.getClientOrders(clientId);

        return ResponseEntity.ok(orders);
    }


    @Operation(summary = "Cancel order", description = "Client cancels order if status is PENDING")
    @ApiResponse(responseCode = "200", description = "Order cancelled")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<Order> cancelOrder(@PathVariable UUID id) {

        Order order = orderService.cancelOrder(id);

        return ResponseEntity.ok(order);
    }


    @Operation(summary = "Get restaurant orders", description = "Cafe admin sees incoming orders")
    @ApiResponse(responseCode = "200", description = "Orders retrieved")
    @GetMapping("/cafe")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<List<Order>> getCafeOrders(@RequestParam UUID restaurantId) {

        List<Order> orders = orderService.getRestaurantOrders(restaurantId);

        return ResponseEntity.ok(orders);
    }


    @Operation(summary = "Confirm order", description = "Cafe confirms order")
    @ApiResponse(responseCode = "200", description = "Order confirmed")
    @PatchMapping("/cafe/{id}/confirm")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<Order> confirmOrder(@PathVariable UUID id) {

        Order order = orderService.confirmOrder(id);

        return ResponseEntity.ok(order);
    }

}