package com.programthis.order_cart_service.controller;

import com.programthis.order_cart_service.model.Order;
import com.programthis.order_cart_service.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Endpoint para crear un pedido a partir del carrito de un usuario
    // POST http://localhost:8083/api/orders/{userId}/createFromCart
    // Body (JSON): { "shippingAddress": "Calle Falsa 123", "paymentMethod": "Credit Card" }
    @PostMapping("/{userId}/createFromCart")
    public ResponseEntity<Order> createOrderFromCart(
            @PathVariable Long userId,
            @RequestBody OrderCreationRequest request) { // Usaremos una clase DTO para el cuerpo de la solicitud
        try {
            Order newOrder = orderService.createOrderFromCart(userId, request.getShippingAddress(), request.getPaymentMethod());
            return new ResponseEntity<>(newOrder, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    // Endpoint para obtener un pedido por su ID
    // GET http://localhost:8083/api/orders/{orderId}
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId)
                .map(order -> new ResponseEntity<>(order, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Endpoint para obtener todos los pedidos de un usuario
    // GET http://localhost:8083/api/orders/user/{userId}
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable Long userId) {
        List<Order> orders = orderService.getOrdersByUserId(userId);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    // Endpoint para actualizar el estado de un pedido
    // PUT http://localhost:8083/api/orders/{orderId}/status
    // Body (JSON): { "newStatus": "SHIPPED" }
    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String newStatus) { // O @RequestBody si el status viene en un JSON más complejo
        try {
            Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
            return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    // (Opcional) Endpoint para eliminar un pedido
    // DELETE http://localhost:8083/api/orders/{orderId}
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        try {
            orderService.deleteOrder(orderId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Clase interna (DTO) para manejar el cuerpo de la solicitud de creación de pedido
    // Puedes crear esto en un nuevo paquete 'dto' si lo prefieres para una mejor organización.
    @Data // Lombok para getters y setters
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCreationRequest {
        private String shippingAddress;
        private String paymentMethod;
    }
}