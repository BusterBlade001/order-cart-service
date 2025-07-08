package com.programthis.order_cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDto {
    private String recipientEmail;
    private String subject;
    private String messageBody;
    private String type; // Ej: "ORDER_CONFIRMATION", "PAYMENT_SUCCESS"
}