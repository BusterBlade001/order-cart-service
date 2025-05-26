package com.programthis.order_cart_service.repository;

import com.programthis.order_cart_service.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Método personalizado para encontrar todos los ítems de un pedido específico
    List<OrderItem> findByOrderId(Long orderId);
}