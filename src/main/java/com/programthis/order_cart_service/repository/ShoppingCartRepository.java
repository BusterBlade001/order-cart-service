package com.programthis.order_cart_service.repository;

import com.programthis.order_cart_service.model.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {
    // Método personalizado para encontrar un carrito por el ID del usuario
    Optional<ShoppingCart> findByUserId(Long userId);
}