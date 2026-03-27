package com.fooddelivery.orders.controller;

import com.fooddelivery.auth.security.CustomUserDetails;
import com.fooddelivery.orders.dto.CancelOrderRequest;
import com.fooddelivery.orders.dto.CreateOrderRequest;
import com.fooddelivery.orders.dto.TrackingInfoDto;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientId = userDetails.getId();
        Order order = orderService.createOrder(request, clientId);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Cancel order", description = "Client cancels order if status is PENDING")
    @ApiResponse(responseCode = "200", description = "Order cancelled")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID clientId = userDetails.getId();
        Order order = orderService.cancelOrderByClient(id, clientId, "Cancelled by client");
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Get restaurant orders", description = "Cafe admin sees incoming orders with filters")
    @ApiResponse(responseCode = "200", description = "Orders retrieved")
    @GetMapping("/cafe")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<Page<Order>> getCafeOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID cafeId = userDetails.getCafeId();
        if (cafeId == null) {
            throw new AccessDeniedException("User is not associated with any cafe");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.getRestaurantOrders(
                cafeId, status, orderNumber, startDate, endDate, minAmount, maxAmount, pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get my orders", description = "Client retrieves their order history with filters")
    @ApiResponse(responseCode = "200", description = "Orders retrieved")
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<Page<Order>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID clientId = userDetails.getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.getClientOrders(
                clientId, status, orderNumber, startDate, endDate, minAmount, maxAmount, pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Confirm order", description = "Cafe confirms order")
    @ApiResponse(responseCode = "200", description = "Order confirmed")
    @PatchMapping("/cafe/{id}/confirm")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<Order> confirmOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID cafeId = userDetails.getCafeId();
        if (cafeId == null) {
            throw new AccessDeniedException("User is not associated with any cafe");
        }
        Order order = orderService.confirmOrder(id, cafeId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CLIENT', 'CAFE_ADMIN')")
    public ResponseEntity<Order> getOrderById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication
    ) {
        UUID userId = userDetails.getId();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        UUID cafeId = userDetails.getCafeId();

        Order order = orderService.getOrderById(id, userId, role, cafeId);
        return ResponseEntity.ok(order);
    }

    private UUID extractCafeId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getCafeId();
        }
        return null;
    }

    @Operation(summary = "Mark order as ready", description = "Cafe marks order as ready for pickup")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order marked as ready"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Invalid order state")
    })
    @PatchMapping("/cafe/{id}/ready")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<Order> markOrderReady(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID cafeId = userDetails.getCafeId();
        if (cafeId == null) {
            throw new AccessDeniedException("User is not associated with any cafe");
        }
        Order order = orderService.markAsReady(id, cafeId);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Cancel order by cafe", description = "Cafe admin cancels order with reason (before DELIVERING)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Cannot cancel order in current state")
    })
    @PatchMapping("/cafe/{id}/cancel")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<Order> cancelOrderByCafe(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID cafeId = userDetails.getCafeId();
        if (cafeId == null) {
            throw new AccessDeniedException("User is not associated with any cafe");
        }
        Order order = orderService.cancelOrderByCafe(id, cafeId, request.getReason());
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Get delivery tracking info", description = "Client retrieves delivery status and tracking URL")
    @ApiResponse(responseCode = "200", description = "Tracking info retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @GetMapping("/{id}/tracking")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<TrackingInfoDto> getTrackingInfo(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID clientId = userDetails.getId();
        TrackingInfoDto trackingInfo = orderService.getTrackingInfo(id, clientId);
        return ResponseEntity.ok(trackingInfo);
    }
}