package com.programthis.order_cart_service.repository;

import com.programthis.order_cart_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Método personalizado para encontrar todos los pedidos de un usuario específico
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
}