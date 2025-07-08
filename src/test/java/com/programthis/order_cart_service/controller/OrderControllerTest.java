package com.programthis.order_cart_service.controller;

import com.programthis.order_cart_service.model.Order;
import com.programthis.order_cart_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private Order mockOrder;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setUserId(1L);
    }

    @Test
    public void testCreateOrderFromCart_Success() {
        // Arrange
        Long userId = 1L;
        OrderController.OrderCreationRequest request = new OrderController.OrderCreationRequest("Fake Street", "Credit Card");
        when(orderService.createOrderFromCart(userId, request.getShippingAddress(), request.getPaymentMethod()))
                .thenReturn(mockOrder);

        // Act
        ResponseEntity<EntityModel<Order>> response = orderController.createOrderFromCart(userId, request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockOrder, response.getBody().getContent());
        assertTrue(response.getBody().getLink("self").isPresent());
    }

    @Test
    public void testCreateOrderFromCart_Failure() {
        // Arrange
        Long userId = 1L;
        OrderController.OrderCreationRequest request = new OrderController.OrderCreationRequest("Fake Street", "Credit Card");
        when(orderService.createOrderFromCart(anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Cart not found"));

        // Act
        ResponseEntity<EntityModel<Order>> response = orderController.createOrderFromCart(userId, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testGetOrderById_Found() {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(mockOrder));

        // Act
        ResponseEntity<EntityModel<Order>> response = orderController.getOrderById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockOrder, response.getBody().getContent());
        assertTrue(response.getBody().getLink("self").isPresent());
    }

    @Test
    public void testGetOrderById_NotFound() {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<EntityModel<Order>> response = orderController.getOrderById(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testGetOrdersByUserId() {
        // Arrange
        Long userId = 1L;
        List<Order> mockOrders = Arrays.asList(mockOrder, new Order());
        when(orderService.getOrdersByUserId(userId)).thenReturn(mockOrders);

        // Act
        ResponseEntity<CollectionModel<EntityModel<Order>>> response = orderController.getOrdersByUserId(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        assertTrue(response.getBody().getLink("self").isPresent());
    }

    @Test
    public void testUpdateOrderStatus_Success() {
        // Arrange
        Long orderId = 1L;
        String newStatus = "SHIPPED";
        Order updatedOrder = new Order();
        updatedOrder.setId(orderId);
        updatedOrder.setUserId(1L);
        updatedOrder.setStatus(newStatus);
        when(orderService.updateOrderStatus(orderId, newStatus)).thenReturn(updatedOrder);

        // Act
        ResponseEntity<EntityModel<Order>> response = orderController.updateOrderStatus(orderId, newStatus);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(newStatus, Objects.requireNonNull(response.getBody().getContent()).getStatus());
    }

    @Test
    public void testUpdateOrderStatus_Failure() {
        // Arrange
        Long orderId = 1L;
        String newStatus = "SHIPPED";
        when(orderService.updateOrderStatus(orderId, newStatus))
                .thenThrow(new RuntimeException("Order not found"));

        // Act
        ResponseEntity<EntityModel<Order>> response = orderController.updateOrderStatus(orderId, newStatus);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testDeleteOrder_Success() {
        // Arrange
        Long orderId = 1L;
        doNothing().when(orderService).deleteOrder(orderId);

        // Act
        ResponseEntity<Void> response = orderController.deleteOrder(orderId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(orderService, times(1)).deleteOrder(orderId);
    }

    @Test
    public void testDeleteOrder_NotFound() {
        // Arrange
        Long orderId = 1L;
        doThrow(new RuntimeException("Order not found")).when(orderService).deleteOrder(orderId);

        // Act
        ResponseEntity<Void> response = orderController.deleteOrder(orderId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}