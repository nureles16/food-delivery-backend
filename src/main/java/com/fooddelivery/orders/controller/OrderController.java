package com.fooddelivery.orders.controller;

import com.fooddelivery.auth.security.CustomUserDetails;
import com.fooddelivery.orders.dto.CancelOrderRequest;
import com.fooddelivery.orders.dto.CreateOrderRequest;
import com.fooddelivery.orders.dto.TrackingInfoDto;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<Order> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID clientId = userDetails.getId();
        Order order = orderService.createOrder(request, clientId);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Cancel order", description = "Client cancels order if status is PENDING or PAID")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID clientId = userDetails.getId();
        Order order = orderService.cancelOrderByClient(id, clientId, request.getReason());
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Get restaurant orders", description = "Cafe admin sees incoming orders with filters")
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
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        UUID cafeId = userDetails.getCafeId();
        if (cafeId == null) {
            throw new AccessDeniedException("User is not associated with any cafe");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.getRestaurantOrders(cafeId, cafeId, status,
                orderNumber, startDate, endDate, minAmount, maxAmount, pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get my orders", description = "Client retrieves their order history")
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
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        UUID clientId = userDetails.getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.getClientOrders(clientId, status, orderNumber,
                startDate, endDate, minAmount, maxAmount, pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Confirm order", description = "Cafe confirms order")
    @PatchMapping("/cafe/{id}/confirm")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<Order> confirmOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID cafeId = userDetails.getCafeId();
        if (cafeId == null) throw new AccessDeniedException("User is not associated with any cafe");
        Order order = orderService.confirmOrder(id, cafeId);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Mark order as cooking", description = "Cafe marks order as being cooked")
    @PatchMapping("/cafe/{id}/cooking")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<Order> markAsCooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID cafeId = userDetails.getCafeId();
        if (cafeId == null) throw new AccessDeniedException("User is not associated with any cafe");
        Order order = orderService.markAsCooking(id, cafeId);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Mark order as ready", description = "Cafe marks order as ready for pickup")
    @PatchMapping("/cafe/{id}/ready")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<Order> markOrderReady(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID cafeId = userDetails.getCafeId();
        if (cafeId == null) throw new AccessDeniedException("User is not associated with any cafe");
        Order order = orderService.markAsReady(id, cafeId);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Cancel order by cafe", description = "Cafe admin cancels order with reason")
    @PatchMapping("/cafe/{id}/cancel")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<Order> cancelOrderByCafe(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID cafeId = userDetails.getCafeId();
        if (cafeId == null) throw new AccessDeniedException("User is not associated with any cafe");
        Order order = orderService.cancelOrderByCafe(id, cafeId, request.getReason());
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Get order details", description = "Client, cafe admin or super admin can view order")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CLIENT', 'CAFE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Order> getOrderById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails.getId();
        String role = userDetails.getRole();
        UUID cafeId = userDetails.getCafeId();
        Order order = orderService.getOrderById(id, userId, role, cafeId);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Get delivery tracking info", description = "Client retrieves delivery status and tracking URL")
    @GetMapping("/{id}/tracking")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<TrackingInfoDto> getTrackingInfo(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID clientId = userDetails.getId();
        TrackingInfoDto trackingInfo = orderService.getTrackingInfo(id, clientId);
        return ResponseEntity.ok(trackingInfo);
    }

    @Operation(summary = "Start delivery manually", description = "Cafe admin starts delivery (fallback)")
    @PatchMapping("/cafe/{id}/start-delivery")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<Order> startDeliveryManually(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID cafeId = userDetails.getCafeId();
        if (cafeId == null) throw new AccessDeniedException("User is not associated with any cafe");
        Order order = orderService.manualStartDelivery(id, cafeId);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Complete delivery manually", description = "Cafe admin marks order as delivered (fallback)")
    @PatchMapping("/cafe/{id}/complete-delivery")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<Order> completeDeliveryManually(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID cafeId = userDetails.getCafeId();
        if (cafeId == null) throw new AccessDeniedException("User is not associated with any cafe");
        Order order = orderService.manualCompleteDelivery(id, cafeId);
        return ResponseEntity.ok(order);
    }

}