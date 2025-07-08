package com.programthis.order_cart_service.service;

import com.programthis.order_cart_service.client.ProductCatalogServiceClient;
import com.programthis.order_cart_service.dto.ProductDto;
import com.programthis.order_cart_service.model.CartItem; // Usando tu modelo real
import com.programthis.order_cart_service.model.Order;
import com.programthis.order_cart_service.model.ShoppingCart; // Usando tu modelo real
import com.programthis.order_cart_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShoppingCartService shoppingCartService;

    @Mock
    private ProductCatalogServiceClient productCatalogServiceClient;

    @InjectMocks
    private OrderService orderService;

    private Long userId;
    private ShoppingCart cart;
    private ProductDto productDto1;
    private ProductDto productDto2;

    @BeforeEach
    void setUp() {
        userId = 1L;

        // --- SECCIÓN MODIFICADA PARA USAR TUS MODELOS REALES ---

        // 1. Crear el carrito usando el constructor sin argumentos y setters
        cart = new ShoppingCart();
        cart.setUserId(userId);

        // 2. Crear el primer CartItem usando el constructor sin argumentos y setters
        CartItem item1 = new CartItem();
        item1.setProductId(101L);
        item1.setQuantity(2);
        item1.setPriceAtAddition(new BigDecimal("10.00"));

        // 3. Crear el segundo CartItem
        CartItem item2 = new CartItem();
        item2.setProductId(102L);
        item2.setQuantity(1);
        item2.setPriceAtAddition(new BigDecimal("25.50"));

        // 4. Usar tu método 'addCartItem' para añadir los ítems al carrito
        cart.addCartItem(item1);
        cart.addCartItem(item2);

        // --- FIN DE LA SECCIÓN MODIFICADA ---


        // Configuración de DTOs de productos de ejemplo (esto no cambia)
        productDto1 = new ProductDto(101L, "Laptop", "Una laptop potente", new BigDecimal("10.00"), 10);
        productDto2 = new ProductDto(102L, "Mouse", "Un mouse inalámbrico", new BigDecimal("25.50"), 50);
    }

    @Test
    void createOrderFromCart_Success() {
        // Arrange
        String shippingAddress = "123 Calle Falsa, Springfield";
        String paymentMethod = "Credit Card";

        when(shoppingCartService.getOrCreateShoppingCart(userId)).thenReturn(cart);
        when(productCatalogServiceClient.getProductById(101L)).thenReturn(Optional.of(productDto1));
        when(productCatalogServiceClient.getProductById(102L)).thenReturn(Optional.of(productDto2));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order createdOrder = orderService.createOrderFromCart(userId, shippingAddress, paymentMethod);

        // Assert
        assertNotNull(createdOrder);
        assertEquals(userId, createdOrder.getUserId());
        assertEquals(shippingAddress, createdOrder.getShippingAddress());
        assertEquals(paymentMethod, createdOrder.getPaymentMethod());
        assertEquals("PENDING", createdOrder.getStatus());
        assertEquals(2, createdOrder.getItems().size());
        assertEquals(new BigDecimal("45.50"), createdOrder.getTotalAmount());
        
        assertEquals("Laptop", createdOrder.getItems().get(0).getProductName());
        assertEquals("Mouse", createdOrder.getItems().get(1).getProductName());

        verify(shoppingCartService, times(1)).getOrCreateShoppingCart(userId);
        verify(productCatalogServiceClient, times(1)).getProductById(101L);
        verify(productCatalogServiceClient, times(1)).getProductById(102L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(shoppingCartService, times(1)).clearCart(userId);
    }

    @Test
    void createOrderFromCart_CartIsEmpty_ShouldThrowException() {
        // Arrange
        ShoppingCart emptyCart = new ShoppingCart(); // Usar constructor vacío
        emptyCart.setUserId(userId);
        
        when(shoppingCartService.getOrCreateShoppingCart(userId)).thenReturn(emptyCart);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrderFromCart(userId, "address", "payment");
        });

        assertEquals("El carrito está vacío. No se puede crear un pedido.", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
        verify(shoppingCartService, never()).clearCart(userId);
    }

    // Los tests restantes de aquí para abajo no necesitaban cambios
    // ya que no dependen de la estructura de CartItem o ShoppingCart.
    
    @Test
    void createOrderFromCart_ProductNotFoundInCatalog_ShouldThrowException() {
        // Arrange
        when(shoppingCartService.getOrCreateShoppingCart(userId)).thenReturn(cart);
        when(productCatalogServiceClient.getProductById(101L)).thenReturn(Optional.of(productDto1));
        when(productCatalogServiceClient.getProductById(102L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrderFromCart(userId, "address", "payment");
        });

        assertEquals("Producto con ID 102 en el carrito no encontrado en el catálogo. No se puede crear el pedido.", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
        verify(shoppingCartService, never()).clearCart(userId);
    }

    @Test
    void getOrderById_OrderExists() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // Act
        Optional<Order> foundOrder = orderService.getOrderById(1L);

        // Assert
        assertTrue(foundOrder.isPresent());
        assertEquals(1L, foundOrder.get().getId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void getOrderById_OrderDoesNotExist() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<Order> foundOrder = orderService.getOrderById(1L);

        // Assert
        assertFalse(foundOrder.isPresent());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void getOrdersByUserId_Success() {
        // Arrange
        Order order1 = new Order();
        order1.setUserId(userId);
        Order order2 = new Order();
        order2.setUserId(userId);
        List<Order> orders = List.of(order1, order2);

        when(orderRepository.findByUserIdOrderByOrderDateDesc(userId)).thenReturn(orders);

        // Act
        List<Order> result = orderService.getOrdersByUserId(userId);

        // Assert
        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findByUserIdOrderByOrderDateDesc(userId);
    }

    @Test
    void updateOrderStatus_Success() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setStatus("PENDING");
        String newStatus = "SHIPPED";

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order updatedOrder = orderService.updateOrderStatus(1L, newStatus);

        // Assert
        assertNotNull(updatedOrder);
        assertEquals(newStatus, updatedOrder.getStatus());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(newStatus, orderCaptor.getValue().getStatus());
    }

    @Test
    void updateOrderStatus_OrderNotFound_ShouldThrowException() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.updateOrderStatus(1L, "PAID");
        });
        assertEquals("Pedido no encontrado: 1", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteOrder_Success() {
        // Arrange
        Long orderId = 1L;
        doNothing().when(orderRepository).deleteById(orderId);

        // Act
        orderService.deleteOrder(orderId);

        // Assert
        verify(orderRepository, times(1)).deleteById(orderId);
    }
}