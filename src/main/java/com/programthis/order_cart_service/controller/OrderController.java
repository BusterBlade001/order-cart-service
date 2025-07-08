package com.programthis.order_cart_service.controller;

import com.programthis.order_cart_service.model.Order;
import com.programthis.order_cart_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "APIs for managing customer orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private EntityModel<Order> toModel(Order order) {
        return EntityModel.of(order,
                linkTo(methodOn(OrderController.class).getOrderById(order.getId())).withSelfRel(),
                linkTo(methodOn(OrderController.class).getOrdersByUserId(order.getUserId())).withRel("user-orders"));
    }
    
    @Operation(summary = "Create an order from a user's cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "400", description = "Bad request, e.g., empty cart")
    })
    @PostMapping("/{userId}/createFromCart")
    public ResponseEntity<EntityModel<Order>> createOrderFromCart(
            @PathVariable Long userId,
            @RequestBody OrderCreationRequest request) {
        try {
            Order newOrder = orderService.createOrderFromCart(userId, request.getShippingAddress(), request.getPaymentMethod());
            EntityModel<Order> orderModel = toModel(newOrder);
            return new ResponseEntity<>(orderModel, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get an order by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the order",
                    content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<EntityModel<Order>> getOrderById(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId)
                .map(order -> ResponseEntity.ok(toModel(order)))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(summary = "Get all orders for a specific user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<CollectionModel<EntityModel<Order>>> getOrdersByUserId(@PathVariable Long userId) {
        List<EntityModel<Order>> orders = orderService.getOrdersByUserId(userId).stream()
                .map(this::toModel)
                .collect(Collectors.toList());

        if (orders.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(CollectionModel.of(orders,
                linkTo(methodOn(OrderController.class).getOrdersByUserId(userId)).withSelfRel()));
    }

    @Operation(summary = "Update the status of an order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order status updated",
                content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Order.class))),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/{orderId}/status")
    public ResponseEntity<EntityModel<Order>> updateOrderStatus(
            @PathVariable Long orderId,
            @Parameter(description = "New status for the order", required = true) @RequestParam String newStatus) {
        try {
            Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
            return ResponseEntity.ok(toModel(updatedOrder));
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Delete an order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        try {
            orderService.deleteOrder(orderId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCreationRequest {
        private String shippingAddress;
        private String paymentMethod;
    }
}