package com.programthis.order_cart_service.controller;

import com.programthis.order_cart_service.model.ShoppingCart;
import com.programthis.order_cart_service.service.ShoppingCartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ShoppingCartControllerTest {

    @Mock
    private ShoppingCartService shoppingCartService;

    @InjectMocks
    private ShoppingCartController shoppingCartController;

    private ShoppingCart cart;
    private Long userId = 1L;
    private Long productId = 100L;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        cart = new ShoppingCart();
        cart.setId(1L);
        cart.setUserId(userId);
    }

    @Test
    public void testGetOrCreateCart() {
        // Arrange
        when(shoppingCartService.getOrCreateShoppingCart(userId)).thenReturn(cart);

        // Act
        ResponseEntity<EntityModel<ShoppingCart>> response = shoppingCartController.getOrCreateCart(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(cart, response.getBody().getContent());
        assertTrue(response.getBody().getLink("self").isPresent());
    }

    @Test
    public void testAddProductToCart_Success() {
        // Arrange
        when(shoppingCartService.addProductToCart(userId, productId, 2)).thenReturn(cart);

        // Act
        ResponseEntity<EntityModel<ShoppingCart>> response = shoppingCartController.addProductToCart(userId, productId, 2);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(cart, response.getBody().getContent());
    }

    @Test
    public void testAddProductToCart_Failure() {
        // Arrange
        when(shoppingCartService.addProductToCart(userId, productId, 2)).thenThrow(new RuntimeException("Product not found"));

        // Act
        ResponseEntity<EntityModel<ShoppingCart>> response = shoppingCartController.addProductToCart(userId, productId, 2);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testUpdateProductQuantityInCart_Success() {
        // Arrange
        when(shoppingCartService.updateProductQuantityInCart(userId, productId, 3)).thenReturn(cart);

        // Act
        ResponseEntity<EntityModel<ShoppingCart>> response = shoppingCartController.updateProductQuantityInCart(userId, productId, 3);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(cart, Objects.requireNonNull(response.getBody()).getContent());
    }

    @Test
    public void testUpdateProductQuantityInCart_Failure() {
        // Arrange
        when(shoppingCartService.updateProductQuantityInCart(userId, productId, 3)).thenThrow(new RuntimeException("Product not found"));

        // Act
        ResponseEntity<EntityModel<ShoppingCart>> response = shoppingCartController.updateProductQuantityInCart(userId, productId, 3);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testRemoveProductFromCart_Success() {
        // Arrange
        when(shoppingCartService.removeProductFromCart(userId, productId)).thenReturn(cart);

        // Act
        ResponseEntity<EntityModel<ShoppingCart>> response = shoppingCartController.removeProductFromCart(userId, productId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(cart, Objects.requireNonNull(response.getBody()).getContent());
    }

    @Test
    public void testRemoveProductFromCart_Failure() {
        // Arrange
        when(shoppingCartService.removeProductFromCart(userId, productId)).thenThrow(new RuntimeException("Product not in cart"));

        // Act
        ResponseEntity<EntityModel<ShoppingCart>> response = shoppingCartController.removeProductFromCart(userId, productId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testClearCart_Success() {
        // Arrange
        when(shoppingCartService.clearCart(userId)).thenReturn(cart);

        // Act
        ResponseEntity<EntityModel<ShoppingCart>> response = shoppingCartController.clearCart(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(cart, Objects.requireNonNull(response.getBody()).getContent());
    }

    @Test
    public void testClearCart_Failure() {
        // Arrange
        when(shoppingCartService.clearCart(userId)).thenThrow(new RuntimeException("Cart not found"));

        // Act
        ResponseEntity<EntityModel<ShoppingCart>> response = shoppingCartController.clearCart(userId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }
}