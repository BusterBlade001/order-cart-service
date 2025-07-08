package com.programthis.order_cart_service.service;

import com.programthis.order_cart_service.model.Order;
import com.programthis.order_cart_service.model.OrderItem;
import com.programthis.order_cart_service.model.ShoppingCart;
import com.programthis.order_cart_service.repository.OrderRepository;
import com.programthis.order_cart_service.repository.OrderItemRepository;
import com.programthis.order_cart_service.client.ProductCatalogServiceClient; // ¡Añadido!
import com.programthis.order_cart_service.client.PaymentServiceClient; // ¡NUEVA ADICIÓN!
import com.programthis.order_cart_service.dto.ProductDto; // ¡Añadido!
import com.programthis.order_cart_service.dto.PaymentRequestDto; // ¡NUEVA ADICIÓN!
import com.programthis.order_cart_service.dto.PaymentResponseDto; // ¡NUEVA ADICIÓN!

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
    private final ShoppingCartService shoppingCartService;
    private final ProductCatalogServiceClient productCatalogServiceClient;
    private final PaymentServiceClient paymentServiceClient; // ¡NUEVA ADICIÓN!

    @Autowired
    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        ShoppingCartService shoppingCartService,
                        ProductCatalogServiceClient productCatalogServiceClient,
                        PaymentServiceClient paymentServiceClient) { // ¡MODIFICACIÓN CLAVE: Inyección de PaymentServiceClient!
        this.orderRepository = orderRepository;
        this.shoppingCartService = shoppingCartService;
        this.productCatalogServiceClient = productCatalogServiceClient;
        this.paymentServiceClient = paymentServiceClient; // ¡NUEVA ADICIÓN!
    }

    // Crear un pedido a partir del carrito de un usuario
    @Transactional
    public Order createOrderFromCart(Long userId, String shippingAddress, String paymentMethod) {
        ShoppingCart cart = shoppingCartService.getOrCreateShoppingCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío. No se puede crear un pedido.");
        }

        Order newOrder = new Order();
        newOrder.setUserId(userId);
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setStatus("PENDING"); // O un estado inicial adecuado
        newOrder.setShippingAddress(shippingAddress);
        newOrder.setPaymentMethod(paymentMethod); // Guardamos el método de pago especificado

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductId(cartItem.getProductId());

                    // *** Obtener el nombre del producto del Product Catalog Service ***
                    Optional<ProductDto> productDtoOptional = productCatalogServiceClient.getProductById(cartItem.getProductId());
                    if (productDtoOptional.isEmpty()) {
                        throw new RuntimeException("Producto con ID " + cartItem.getProductId() + " en el carrito no encontrado en el catálogo. No se puede crear el pedido.");
                    }
                    ProductDto productDto = productDtoOptional.get();
                    orderItem.setProductName(productDto.getName()); // Usar el nombre real del producto

                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setUnitPrice(cartItem.getPriceAtAddition()); // Usar el precio que se guardó en el carrito
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
        
        // Limpiar el carrito después de crear el pedido
        shoppingCartService.clearCart(userId);

        // ¡MODIFICACIÓN CLAVE: Iniciar el proceso de pago!
        PaymentRequestDto paymentRequest = new PaymentRequestDto(
                savedOrder.getId().toString(), // El Payment Service espera orderId como String
                savedOrder.getTotalAmount(),
                savedOrder.getPaymentMethod() // Usa el método de pago de la orden
        );

        Optional<PaymentResponseDto> paymentResponseOptional = paymentServiceClient.processPayment(paymentRequest);

        if (paymentResponseOptional.isPresent()) {
            PaymentResponseDto paymentResponse = paymentResponseOptional.get();
            // Actualiza el estado de la orden basándose en la respuesta del servicio de pagos
            savedOrder.setStatus(paymentResponse.getPaymentStatus()); 
            savedOrder.setTransactionId(paymentResponse.getTransactionId()); // Guarda el ID de transacción
            orderRepository.save(savedOrder); // Guarda la orden actualizada con el estado de pago
            System.out.println("Pago para orden " + savedOrder.getId() + " procesado con estado: " + paymentResponse.getPaymentStatus());
        } else {
            // Manejar el caso donde el pago no se pudo procesar (Optional.empty() de PaymentServiceClient)
            System.err.println("Error: El pago para la orden " + savedOrder.getId() + " no pudo ser procesado por el Payment Service.");
            // Podrías actualizar el estado de la orden a un estado de error, o lanzar una excepción.
            savedOrder.setStatus("PAYMENT_FAILED"); // Ejemplo de actualización a estado de fallo
            orderRepository.save(savedOrder);
            throw new RuntimeException("El pago para la orden " + savedOrder.getId() + " falló o no pudo ser procesado.");
        }

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