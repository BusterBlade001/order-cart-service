package com.programthis.order_cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private Long id;
    private String orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus; // e.g., "COMPLETED", "FAILED", "PENDING"
    private String transactionId;
    private LocalDateTime transactionDate;
}