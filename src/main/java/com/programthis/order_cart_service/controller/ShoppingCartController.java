package com.programthis.order_cart_service.controller;

import com.programthis.order_cart_service.model.ShoppingCart;
import com.programthis.order_cart_service.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Ya no necesitamos importar BigDecimal aquí, ya que el controlador no lo recibe directamente
// import java.math.BigDecimal;

@RestController
@RequestMapping("/api/carts")
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    @Autowired
    public ShoppingCartController(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    // Endpoint para obtener o crear un carrito por ID de usuario
    // GET http://localhost:8083/api/carts/{userId}
    @GetMapping("/{userId}")
    public ResponseEntity<ShoppingCart> getOrCreateCart(@PathVariable Long userId) {
        ShoppingCart cart = shoppingCartService.getOrCreateShoppingCart(userId);
        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    // Endpoint para añadir un producto al carrito
    // POST http://localhost:8083/api/carts/{userId}/items
    // Los parámetros se envían como Query Parameters en la URL (ej. ?productId=101&quantity=2)
    @PostMapping("/{userId}/items")
    public ResponseEntity<ShoppingCart> addProductToCart(
            @PathVariable Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        try {
            // El precio se obtiene internamente en el ShoppingCartService
            ShoppingCart updatedCart = shoppingCartService.addProductToCart(userId, productId, quantity);
            return new ResponseEntity<>(updatedCart, HttpStatus.OK);
        } catch (RuntimeException e) {
            // Imprime el error para depuración
            System.err.println("Error al añadir producto al carrito: " + e.getMessage());
            // Devuelve un error 400 Bad Request si el producto no se encontró, etc.
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    // Endpoint para actualizar la cantidad de un producto en el carrito
    // PUT http://localhost:8083/api/carts/{userId}/items/{productId}
    // El nuevoQuantity se envía como Query Parameter (ej. ?newQuantity=3)
    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<ShoppingCart> updateProductQuantityInCart(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestParam Integer newQuantity) {
        try {
            ShoppingCart updatedCart = shoppingCartService.updateProductQuantityInCart(userId, productId, newQuantity);
            return new ResponseEntity<>(updatedCart, HttpStatus.OK);
        } catch (RuntimeException e) {
            System.err.println("Error al actualizar cantidad del producto en el carrito: " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND); // 404 si el carrito o producto no existe
        }
    }

    // Endpoint para eliminar un producto del carrito
    // DELETE http://localhost:8083/api/carts/{userId}/items/{productId}
    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<ShoppingCart> removeProductFromCart(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        try {
            ShoppingCart updatedCart = shoppingCartService.removeProductFromCart(userId, productId);
            return new ResponseEntity<>(updatedCart, HttpStatus.OK);
        } catch (RuntimeException e) {
            System.err.println("Error al eliminar producto del carrito: " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND); // 404 si el carrito o producto no existe
        }
    }

    // Endpoint para vaciar el carrito
    // DELETE http://localhost:8083/api/carts/{userId}/clear
    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<ShoppingCart> clearCart(@PathVariable Long userId) {
        try {
            ShoppingCart clearedCart = shoppingCartService.clearCart(userId);
            return new ResponseEntity<>(clearedCart, HttpStatus.OK);
        } catch (RuntimeException e) {
            System.err.println("Error al vaciar el carrito: " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND); // 404 si el carrito no existe
        }
    }
}