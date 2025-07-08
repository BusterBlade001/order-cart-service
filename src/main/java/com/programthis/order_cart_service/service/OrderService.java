package com.programthis.order_cart_service.service;

import com.programthis.order_cart_service.model.Order;
import com.programthis.order_cart_service.model.OrderItem;
import com.programthis.order_cart_service.model.ShoppingCart;
import com.programthis.order_cart_service.repository.OrderRepository;
import com.programthis.order_cart_service.repository.OrderItemRepository;
import com.programthis.order_cart_service.client.ProductCatalogServiceClient;
import com.programthis.order_cart_service.client.PaymentServiceClient;
import com.programthis.order_cart_service.client.NotificationServiceClient; // ¡NUEVA ADICIÓN!
import com.programthis.order_cart_service.dto.ProductDto;
import com.programthis.order_cart_service.dto.PaymentRequestDto;
import com.programthis.order_cart_service.dto.PaymentResponseDto;
import com.programthis.order_cart_service.dto.NotificationRequestDto; // ¡NUEVA ADICIÓN!

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
    private final PaymentServiceClient paymentServiceClient;
    private final NotificationServiceClient notificationServiceClient; // ¡NUEVA ADICIÓN!

    @Autowired
    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        ShoppingCartService shoppingCartService,
                        ProductCatalogServiceClient productCatalogServiceClient,
                        PaymentServiceClient paymentServiceClient,
                        NotificationServiceClient notificationServiceClient) { // ¡MODIFICACIÓN CLAVE: Inyección de NotificationServiceClient!
        this.orderRepository = orderRepository;
        this.shoppingCartService = shoppingCartService;
        this.productCatalogServiceClient = productCatalogServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.notificationServiceClient = notificationServiceClient; // ¡NUEVA ADICIÓN!
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
        newOrder.setPaymentMethod(paymentMethod);

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
                    orderItem.setProductName(productDto.getName());

                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setUnitPrice(cartItem.getPriceAtAddition());
                    orderItem.setSubtotal(cartItem.getPriceAtAddition().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                    orderItem.setOrder(newOrder);
                    return orderItem;
                })
                .collect(Collectors.toList());

        for (OrderItem item : orderItems) {
            totalAmount = totalAmount.add(item.getSubtotal());
            newOrder.addOrderItem(item);
        }

        newOrder.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(newOrder);
        
        shoppingCartService.clearCart(userId);

        PaymentRequestDto paymentRequest = new PaymentRequestDto(
                savedOrder.getId().toString(),
                savedOrder.getTotalAmount(),
                savedOrder.getPaymentMethod()
        );

        Optional<PaymentResponseDto> paymentResponseOptional = paymentServiceClient.processPayment(paymentRequest);

        if (paymentResponseOptional.isPresent()) {
            PaymentResponseDto paymentResponse = paymentResponseOptional.get();
            savedOrder.setStatus(paymentResponse.getPaymentStatus()); 
            savedOrder.setTransactionId(paymentResponse.getTransactionId());
            orderRepository.save(savedOrder);
            System.out.println("Pago para orden " + savedOrder.getId() + " procesado con estado: " + paymentResponse.getPaymentStatus());

            // ¡NUEVA ADICIÓN CLAVE: Enviar notificación de confirmación de orden!
            if ("COMPLETED".equals(paymentResponse.getPaymentStatus())) {
                // Aquí necesitarías el email real del usuario. Por ahora, un placeholder.
                // EL SIGUIENTE PASO ES CONECTARSE CON USER-SERVICE PARA OBTENER EL EMAIL
                String userEmail = "usuario" + userId + "@example.com"; // Placeholder
                String subject = "Confirmación de Orden #" + savedOrder.getId();
                String messageBody = String.format("Estimado cliente,\n\nGracias por su compra. Su orden #%d ha sido confirmada y su pago ha sido procesado con éxito. Total: %.2f\n\nSaludos,\nEl equipo de EcoMarket", savedOrder.getId(), savedOrder.getTotalAmount());
                
                NotificationRequestDto notificationRequest = new NotificationRequestDto(
                    userEmail,
                    subject,
                    messageBody,
                    "ORDER_CONFIRMATION" // Tipo de notificación
                );
                
                try {
                    boolean notificationSent = notificationServiceClient.sendEmailNotification(notificationRequest);
                    if (notificationSent) {
                        System.out.println("Notificación de confirmación de orden enviada para el usuario " + userId);
                    } else {
                        System.err.println("Fallo al enviar notificación de confirmación para el usuario " + userId);
                    }
                } catch (Exception e) {
                    System.err.println("Excepción al intentar enviar notificación de confirmación para el usuario " + userId + ": " + e.getMessage());
                }
            }

        } else {
            System.err.println("Error: El pago para la orden " + savedOrder.getId() + " no pudo ser procesado por el Payment Service.");
            savedOrder.setStatus("PAYMENT_FAILED");
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