package com.fooddelivery.orders.controller;

import com.fooddelivery.orders.dto.*;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Operation(summary = "Создать заказ", description = "Клиент создаёт новый заказ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ успешно создан",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные (пустые позиции, адрес вне зоны доставки и т.д.)"),
            @ApiResponse(responseCode = "403", description = "Только клиенты могут создавать заказы"),
            @ApiResponse(responseCode = "404", description = "Ресторан или позиция меню не найдены"),
            @ApiResponse(responseCode = "409", description = "Блюдо недоступно или ресторан закрыт")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @Operation(summary = "Отменить заказ (клиент)", description = "Клиент отменяет свой заказ, если статус PENDING или PAID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ отменён"),
            @ApiResponse(responseCode = "403", description = "Нет прав на отмену этого заказа"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден"),
            @ApiResponse(responseCode = "409", description = "Невозможно отменить заказ в текущем статусе")
    })
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "ID заказа", required = true) @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancelOrderByClient(id, request.getReason()));
    }

    @Operation(summary = "Получить заказы ресторана", description = "Админ ресторана просматривает заказы с фильтрацией")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список заказов получен"),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры фильтрации"),
            @ApiResponse(responseCode = "403", description = "Доступ только для CAFE_ADMIN")
    })
    @GetMapping("/cafe")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getCafeOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderResponse> orders = orderService.getRestaurantOrders(status, orderNumber,
                startDate, endDate, minAmount, maxAmount, pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Получить свои заказы", description = "Клиент просматривает историю своих заказов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список заказов получен"),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры фильтрации"),
            @ApiResponse(responseCode = "403", description = "Доступ только для CLIENT")
    })
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderResponse> orders = orderService.getClientOrders(status, orderNumber,
                startDate, endDate, minAmount, maxAmount, pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Подтвердить заказ", description = "Админ ресторана подтверждает заказ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ подтверждён"),
            @ApiResponse(responseCode = "403", description = "Нет прав или заказ не принадлежит ресторану"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден"),
            @ApiResponse(responseCode = "409", description = "Заказ не в статусе PAID")
    })
    @PatchMapping("/cafe/{id}/confirm")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.confirmOrder(id));
    }

    @Operation(summary = "Начать готовку", description = "Админ ресторана переводит заказ в статус COOKING")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Готовка начата"),
            @ApiResponse(responseCode = "409", description = "Заказ не в статусе CONFIRMED")
    })
    @PatchMapping("/cafe/{id}/cooking")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<OrderResponse> markAsCooking(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.markAsCooking(id));
    }

    @Operation(summary = "Отметить готовность", description = "Заказ готов к выдаче")
    @PatchMapping("/cafe/{id}/ready")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<OrderResponse> markOrderReady(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.markAsReady(id));
    }

    @Operation(summary = "Отменить заказ (ресторан)", description = "Админ ресторана отменяет заказ с указанием причины")
    @PatchMapping("/cafe/{id}/cancel")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<OrderResponse> cancelOrderByCafe(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancelOrderByCafe(id, request.getReason()));
    }

    @Operation(summary = "Детали заказа", description = "Клиент, админ ресторана или супер-админ могут просмотреть заказ")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CLIENT', 'CAFE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @Operation(summary = "Трекинг доставки", description = "Клиент получает статус доставки и URL отслеживания")
    @GetMapping("/{id}/tracking")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<TrackingInfoDto> getTrackingInfo(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getTrackingInfo(id));
    }

    @Operation(summary = "Ручное начало доставки", description = "Для заказов без интеграции с Яндекс.Доставкой")
    @PatchMapping("/cafe/{id}/start-delivery")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<OrderResponse> startDeliveryManually(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.manualStartDelivery(id));
    }

    @Operation(summary = "Ручное завершение доставки", description = "Для заказов без интеграции с Яндекс.Доставкой")
    @PatchMapping("/cafe/{id}/complete-delivery")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<OrderResponse> completeDeliveryManually(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.manualCompleteDelivery(id));
    }

    @Operation(summary = "Назначить курьера (системно)", description = "Вызывается Яндекс.Доставкой или интеграционным модулем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Курьер назначен"),
            @ApiResponse(responseCode = "400", description = "Заказ не в статусе READY"),
            @ApiResponse(responseCode = "403", description = "Доступ только для сервисных аккаунтов (внутренний вызов)")
    })
    @PostMapping("/{id}/assign-courier")
    @PreAuthorize("hasAuthority('SYSTEM_INTEGRATION')") // предполагается роль для сервисных вызовов
    public ResponseEntity<OrderResponse> assignCourier(
            @PathVariable UUID id,
            @RequestParam String yandexDeliveryId,
            @RequestParam(required = false) String trackingUrl,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime estimatedAt) {
        return ResponseEntity.ok(orderService.assignCourierSystem(id, yandexDeliveryId, trackingUrl, estimatedAt));
    }

    @Operation(summary = "Начать доставку (автоматически)", description = "Вебхук от Яндекс.Доставки")
    @PostMapping("/{id}/start-delivery-auto")
    @PreAuthorize("hasAuthority('SYSTEM_INTEGRATION')")
    public ResponseEntity<OrderResponse> startDeliveryAuto(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.startDeliveryAutomatically(id));
    }

    @Operation(summary = "Завершить доставку (автоматически)", description = "Вебхук от Яндекс.Доставки")
    @PostMapping("/{id}/complete-delivery-auto")
    @PreAuthorize("hasAuthority('SYSTEM_INTEGRATION')")
    public ResponseEntity<OrderResponse> completeDeliveryAuto(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.completeDeliveryAutomatically(id));
    }

    @Operation(summary = "Ручное назначение курьера (без интеграции)", description = "Админ ресторана назначает курьера вручную")
    @PostMapping("/cafe/{id}/assign-courier-manual")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<OrderResponse> manualAssignCourier(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.manualAssignCourier(id));
    }

    @Operation(summary = "Отметить заказ как оплаченный", description = "Вызывается платёжным шлюзом")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ оплачен"),
            @ApiResponse(responseCode = "400", description = "Сумма платежа не совпадает"),
            @ApiResponse(responseCode = "409", description = "Заказ уже оплачен или не в статусе PENDING")
    })
    @PostMapping("/{id}/paid")
    @PreAuthorize("hasAuthority('SYSTEM_INTEGRATION') or hasAuthority('CLIENT')")
    public ResponseEntity<OrderResponse> markAsPaid(
            @PathVariable UUID id,
            @RequestParam UUID paymentId,
            @RequestParam BigDecimal paidAmount) {
        return ResponseEntity.ok(orderService.markAsPaid(id, paymentId, paidAmount));
    }

    @Operation(summary = "Отметить оплату как неудавшуюся", description = "Вызывается платёжным шлюзом")
    @PostMapping("/{id}/payment-failed")
    @PreAuthorize("hasAuthority('SYSTEM_INTEGRATION') or hasAuthority('CLIENT')")
    public ResponseEntity<OrderResponse> markPaymentFailed(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(orderService.markPaymentFailed(id, reason));
    }

    @Operation(summary = "Отметить заказ как возвращённый (refunded)", description = "Только для SUPER_ADMIN")
    @PatchMapping("/admin/{id}/refunded")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<OrderResponse> markAsRefunded(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.markAsRefunded(id));
    }
}