package com.programthis.order_cart_service.service;

import com.programthis.order_cart_service.model.CartItem;
import com.programthis.order_cart_service.model.ShoppingCart;
import com.programthis.order_cart_service.repository.CartItemRepository;
import com.programthis.order_cart_service.repository.ShoppingCartRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final CartItemRepository cartItemRepository;

    @Autowired
    public ShoppingCartService(ShoppingCartRepository shoppingCartRepository, CartItemRepository cartItemRepository) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    // Obtener o crear un carrito para un usuario
    @Transactional
    public ShoppingCart getOrCreateShoppingCart(Long userId) {
        // Busca un carrito existente por userId
        Optional<ShoppingCart> existingCart = shoppingCartRepository.findByUserId(userId);

        if (existingCart.isPresent()) {
            return existingCart.get();
        } else {
            // Si no existe, crea uno nuevo
            ShoppingCart newCart = new ShoppingCart();
            newCart.setUserId(userId);
            return shoppingCartRepository.save(newCart);
        }
    }

    // Añadir producto al carrito
    @Transactional
    public ShoppingCart addProductToCart(Long userId, Long productId, Integer quantity, BigDecimal priceAtAddition) {
        ShoppingCart cart = getOrCreateShoppingCart(userId);

        // Buscar si el producto ya está en el carrito
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            // Si ya existe, actualizar la cantidad
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            // Si no existe, crear un nuevo item de carrito
            CartItem newItem = new CartItem();
            newItem.setProductId(productId);
            newItem.setQuantity(quantity);
            newItem.setPriceAtAddition(priceAtAddition);
            newItem.setCart(cart); // Establece la relación bidireccional
            cart.addCartItem(newItem); // Añade al carrito y actualiza la relación
            cartItemRepository.save(newItem);
        }
        return shoppingCartRepository.save(cart); // Guarda el carrito para actualizar updated_at
    }

    // Actualizar cantidad de un producto en el carrito
    @Transactional
    public ShoppingCart updateProductQuantityInCart(Long userId, Long productId, Integer newQuantity) {
        ShoppingCart cart = shoppingCartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado para el usuario: " + userId));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            if (newQuantity <= 0) {
                // Si la nueva cantidad es 0 o menos, eliminar el ítem del carrito
                cart.removeCartItem(item);
                cartItemRepository.delete(item);
            } else {
                item.setQuantity(newQuantity);
                cartItemRepository.save(item);
            }
            return shoppingCartRepository.save(cart); // Guarda el carrito para actualizar updated_at
        } else {
            throw new RuntimeException("Producto no encontrado en el carrito para actualizar: " + productId);
        }
    }

    // Eliminar un producto del carrito
    @Transactional
    public ShoppingCart removeProductFromCart(Long userId, Long productId) {
        ShoppingCart cart = shoppingCartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado para el usuario: " + userId));

        Optional<CartItem> itemToRemove = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (itemToRemove.isPresent()) {
            CartItem item = itemToRemove.get();
            cart.removeCartItem(item); // Elimina del carrito y actualiza la relación
            cartItemRepository.delete(item); // Elimina el item de la base de datos
            return shoppingCartRepository.save(cart); // Guarda el carrito para actualizar updated_at
        } else {
            throw new RuntimeException("Producto no encontrado en el carrito para eliminar: " + productId);
        }
    }

    // Vaciar el carrito
    @Transactional
    public ShoppingCart clearCart(Long userId) {
        ShoppingCart cart = shoppingCartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado para el usuario: " + userId));

        cartItemRepository.deleteAll(cart.getItems()); // Elimina todos los ítems del carrito
        cart.getItems().clear(); // Limpia la lista en memoria
        return shoppingCartRepository.save(cart); // Guarda el carrito para actualizar updated_at
    }
}