package com.programthis.order_cart_service.controller;

import com.programthis.order_cart_service.model.ShoppingCart;
import com.programthis.order_cart_service.service.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/carts")
@Tag(name = "Shopping Cart Management", description = "APIs for managing user shopping carts")
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    @Autowired
    public ShoppingCartController(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    private EntityModel<ShoppingCart> toModel(ShoppingCart cart) {
        return EntityModel.of(cart,
                linkTo(methodOn(ShoppingCartController.class).getOrCreateCart(cart.getUserId())).withSelfRel(),
                linkTo(methodOn(ShoppingCartController.class).addProductToCart(cart.getUserId(), null, null)).withRel("add-item"),
                linkTo(methodOn(ShoppingCartController.class).clearCart(cart.getUserId())).withRel("clear-cart"));
    }

    @Operation(summary = "Get or create a shopping cart for a user")
    @GetMapping("/{userId}")
    public ResponseEntity<EntityModel<ShoppingCart>> getOrCreateCart(@PathVariable Long userId) {
        ShoppingCart cart = shoppingCartService.getOrCreateShoppingCart(userId);
        return new ResponseEntity<>(toModel(cart), HttpStatus.OK);
    }

    @Operation(summary = "Add a product to the cart")
    @PostMapping("/{userId}/items")
    public ResponseEntity<EntityModel<ShoppingCart>> addProductToCart(
            @PathVariable Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        try {
            ShoppingCart updatedCart = shoppingCartService.addProductToCart(userId, productId, quantity);
            return new ResponseEntity<>(toModel(updatedCart), HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update product quantity in the cart")
    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<EntityModel<ShoppingCart>> updateProductQuantityInCart(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestParam Integer newQuantity) {
        try {
            ShoppingCart updatedCart = shoppingCartService.updateProductQuantityInCart(userId, productId, newQuantity);
            return new ResponseEntity<>(toModel(updatedCart), HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Remove a product from the cart")
    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<EntityModel<ShoppingCart>> removeProductFromCart(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        try {
            ShoppingCart updatedCart = shoppingCartService.removeProductFromCart(userId, productId);
            return new ResponseEntity<>(toModel(updatedCart), HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Clear all items from the cart")
    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<EntityModel<ShoppingCart>> clearCart(@PathVariable Long userId) {
        try {
            ShoppingCart clearedCart = shoppingCartService.clearCart(userId);
            return new ResponseEntity<>(toModel(clearedCart), HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}