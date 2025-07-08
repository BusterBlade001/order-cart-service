package com.programthis.order_cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    private String orderId;
    private BigDecimal amount;
    private String paymentMethodDetails; // Por ejemplo, "Credit Card", "Debit Card", etc.
}