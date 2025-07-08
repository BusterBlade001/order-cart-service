package com.programthis.order_cart_service.service;

import com.programthis.order_cart_service.client.ProductCatalogServiceClient;
import com.programthis.order_cart_service.dto.ProductDto;
import com.programthis.order_cart_service.model.CartItem;
import com.programthis.order_cart_service.model.ShoppingCart;
import com.programthis.order_cart_service.repository.CartItemRepository;
import com.programthis.order_cart_service.repository.ShoppingCartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceTest {

    @Mock
    private ShoppingCartRepository shoppingCartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductCatalogServiceClient productCatalogServiceClient;

    @InjectMocks
    private ShoppingCartService shoppingCartService;

    private Long userId;
    private Long productId;
    private ShoppingCart cart;
    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        userId = 1L;
        productId = 101L;

        cart = new ShoppingCart();
        cart.setId(1L);
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>()); 

        productDto = new ProductDto(productId, "Teclado Mecánico", "Un teclado para programar.", new BigDecimal("75.00"), 20);
    }

    @Test
    void getOrCreateShoppingCart_shouldReturnExistingCart() {
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        ShoppingCart result = shoppingCartService.getOrCreateShoppingCart(userId);
        assertNotNull(result);
        assertEquals(cart.getId(), result.getId());
        verify(shoppingCartRepository, times(1)).findByUserId(userId);
        verify(shoppingCartRepository, never()).save(any(ShoppingCart.class));
    }

    @Test
    void getOrCreateShoppingCart_shouldCreateNewCart() {
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(cart);
        ShoppingCart result = shoppingCartService.getOrCreateShoppingCart(userId);
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(shoppingCartRepository, times(1)).findByUserId(userId);
        verify(shoppingCartRepository, times(1)).save(any(ShoppingCart.class));
    }

    @Test
    void addProductToCart_addNewProduct_success() {
        Integer quantity = 2;
        when(productCatalogServiceClient.getProductById(productId)).thenReturn(Optional.of(productDto));
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenAnswer(i -> i.getArgument(0));
        ShoppingCart result = shoppingCartService.addProductToCart(userId, productId, quantity);
        assertEquals(1, result.getItems().size());
        CartItem addedItem = result.getItems().get(0);
        assertEquals(productId, addedItem.getProductId());
        assertEquals(quantity, addedItem.getQuantity());
        assertEquals(new BigDecimal("75.00"), addedItem.getPriceAtAddition());
        verify(productCatalogServiceClient, times(1)).getProductById(productId);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
        verify(shoppingCartRepository, times(1)).save(cart);
    }

    @Test
    void addProductToCart_updateExistingProductQuantity_success() {
        Integer initialQuantity = 1;
        Integer addedQuantity = 2;
        CartItem existingItem = new CartItem();
        existingItem.setProductId(productId);
        existingItem.setQuantity(initialQuantity);
        cart.addCartItem(existingItem);
        when(productCatalogServiceClient.getProductById(productId)).thenReturn(Optional.of(productDto));
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenAnswer(i -> i.getArgument(0));
        ShoppingCart result = shoppingCartService.addProductToCart(userId, productId, addedQuantity);
        assertEquals(1, result.getItems().size());
        assertEquals(initialQuantity + addedQuantity, result.getItems().get(0).getQuantity());
        verify(cartItemRepository, times(1)).save(existingItem);
        verify(shoppingCartRepository, times(1)).save(cart);
    }

    @Test
    void addProductToCart_productNotFound_shouldThrowException() {
        when(productCatalogServiceClient.getProductById(productId)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> shoppingCartService.addProductToCart(userId, productId, 1));
        assertEquals("Producto con ID " + productId + " no encontrado en el catálogo. No se puede añadir al carrito.", exception.getMessage());
        verify(shoppingCartRepository, never()).save(any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateProductQuantityInCart_updateQuantity_success() {
        Integer newQuantity = 5;
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(2);
        cart.addCartItem(item);
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(shoppingCartRepository.save(cart)).thenReturn(cart);
        shoppingCartService.updateProductQuantityInCart(userId, productId, newQuantity);
        assertEquals(newQuantity, item.getQuantity());
        verify(cartItemRepository, times(1)).save(item);
        verify(shoppingCartRepository, times(1)).save(cart);
    }

    @Test
    void updateProductQuantityInCart_removeProductWhenQuantityIsZero_success() {
        Integer newQuantity = 0;
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(2);
        cart.addCartItem(item);
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(shoppingCartRepository.save(cart)).thenReturn(cart);
        doNothing().when(cartItemRepository).delete(item);
        shoppingCartService.updateProductQuantityInCart(userId, productId, newQuantity);
        assertTrue(cart.getItems().isEmpty());
        verify(cartItemRepository, times(1)).delete(item);
        verify(shoppingCartRepository, times(1)).save(cart);
    }

    @Test
    void updateProductQuantityInCart_ProductNotFound_ShouldThrowException() {
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> shoppingCartService.updateProductQuantityInCart(userId, 999L, 5));
        assertEquals("Producto con ID 999 no encontrado en el carrito para actualizar.", exception.getMessage());
        verify(cartItemRepository, never()).save(any());
    }
    
    // --- TEST AÑADIDO PARA CUBRIR orElseThrow ---
    @Test
    void updateProductQuantityInCart_CartNotFound_ShouldThrowException() {
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> shoppingCartService.updateProductQuantityInCart(userId, productId, 5));
        assertEquals("Carrito no encontrado para el usuario: " + userId, exception.getMessage());
    }

    @Test
    void removeProductFromCart_success() {
        CartItem item = new CartItem();
        item.setProductId(productId);
        cart.addCartItem(item);
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        doNothing().when(cartItemRepository).delete(item);
        when(shoppingCartRepository.save(cart)).thenReturn(cart);
        ShoppingCart result = shoppingCartService.removeProductFromCart(userId, productId);
        assertNotNull(result, "El resultado no debería ser nulo");
        assertTrue(result.getItems().isEmpty());
        verify(cartItemRepository, times(1)).delete(item);
        verify(shoppingCartRepository, times(1)).save(cart);
    }

    @Test
    void removeProductFromCart_ProductNotFound_ShouldThrowException() {
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> shoppingCartService.removeProductFromCart(userId, 999L));
        assertEquals("Producto con ID 999 no encontrado en el carrito para eliminar.", exception.getMessage());
        verify(cartItemRepository, never()).delete(any());
    }

    // --- TEST AÑADIDO PARA CUBRIR orElseThrow ---
    @Test
    void removeProductFromCart_CartNotFound_ShouldThrowException() {
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> shoppingCartService.removeProductFromCart(userId, productId));
        assertEquals("Carrito no encontrado para el usuario: " + userId, exception.getMessage());
    }

    @Test
    void clearCart_WithItems_Success() {
        CartItem item1 = new CartItem(); item1.setProductId(101L);
        cart.addCartItem(item1);
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(shoppingCartRepository.save(cart)).thenReturn(cart);
        doNothing().when(cartItemRepository).deleteAll(any());
        shoppingCartService.clearCart(userId);
        verify(cartItemRepository, times(1)).deleteAll(cart.getItems());
        verify(shoppingCartRepository, times(1)).save(cart);
    }

    @Test
    void clearCart_EmptyCart_Success() {
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(shoppingCartRepository.save(cart)).thenReturn(cart);
        shoppingCartService.clearCart(userId);
        verify(cartItemRepository, never()).deleteAll(any());
        verify(shoppingCartRepository, times(1)).save(cart);
    }
    
    // --- TEST AÑADIDO PARA CUBRIR orElseThrow ---
    @Test
    void clearCart_CartNotFound_ShouldThrowException() {
        when(shoppingCartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> shoppingCartService.clearCart(userId));
        assertEquals("Carrito no encontrado para el usuario: " + userId, exception.getMessage());
    }
}