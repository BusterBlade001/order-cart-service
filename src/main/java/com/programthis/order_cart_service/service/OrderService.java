package com.programthis.order_cart_service.service;

import com.programthis.order_cart_service.model.Order;
import com.programthis.order_cart_service.model.OrderItem;
import com.programthis.order_cart_service.model.ShoppingCart;
import com.programthis.order_cart_service.repository.OrderRepository;
import com.programthis.order_cart_service.repository.OrderItemRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShoppingCartService shoppingCartService; // Para interactuar con el carrito

    @Autowired
    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, ShoppingCartService shoppingCartService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shoppingCartService = shoppingCartService;
    }

    // Crear un pedido a partir del carrito de un usuario
    @Transactional
    public Order createOrderFromCart(Long userId, String shippingAddress, String paymentMethod) {
        ShoppingCart cart = shoppingCartService.getOrCreateShoppingCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío. No se puede crear un pedido.");
        }

        // Crear el nuevo pedido
        Order newOrder = new Order();
        newOrder.setUserId(userId);
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setStatus("PENDING"); // O un estado inicial adecuado
        newOrder.setShippingAddress(shippingAddress);
        newOrder.setPaymentMethod(paymentMethod);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductId(cartItem.getProductId());
                    // Nota: Aquí necesitaríamos el nombre del producto.
                    // En un sistema real, haríamos una llamada al Product Catalog Service
                    // para obtener los detalles del producto (nombre, descripción, etc.)
                    // Por ahora, pondremos un placeholder o podríamos añadir un campo al CartItem si es necesario.
                    orderItem.setProductName("Producto ID: " + cartItem.getProductId()); // Placeholder
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setUnitPrice(cartItem.getPriceAtAddition()); // Precio al momento de añadir al carrito
                    orderItem.setSubtotal(cartItem.getPriceAtAddition().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                    orderItem.setOrder(newOrder); // Establece la relación bidireccional
                    return orderItem;
                })
                .collect(Collectors.toList());

        for (OrderItem item : orderItems) {
            totalAmount = totalAmount.add(item.getSubtotal());
            newOrder.addOrderItem(item); // Añade al pedido y actualiza la relación
        }

        newOrder.setTotalAmount(totalAmount);

        // Guardar el pedido y sus ítems
        Order savedOrder = orderRepository.save(newOrder);
        orderItemRepository.saveAll(orderItems); // Guarda los ítems del pedido

        // Limpiar el carrito después de crear el pedido
        shoppingCartService.clearCart(userId);

        return savedOrder;
    }

    // Obtener un pedido por su ID
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    // Obtener todos los pedidos de un usuario
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    // Actualizar el estado de un pedido (ej: de PENDING a PAID, SHIPPED, etc.)
    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + orderId));
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    // (Opcional) Eliminar un pedido - tener cuidado con esto en producción
    @Transactional
    public void deleteOrder(Long orderId) {
        orderRepository.deleteById(orderId);
    }
}