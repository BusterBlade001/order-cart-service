package com.programthis.order_cart_service.repository;

import com.programthis.order_cart_service.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Método personalizado para encontrar todos los ítems de un carrito específico
    List<CartItem> findByCartId(Long cartId);
}