package com.programthis.order_cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email; // ¡Este es clave para las notificaciones!
    private String fullName;
    // Puedes añadir otros campos si los necesitas para la orden (ej. dirección)
    // private String shippingAddress;
    // private String billingAddress;
}