package de.asel.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String orderId;        // Unique order identifier (crucial for idempotency)
    private String customerId;     // Customer identifier
    private String productCategory;// e.g. "Electronics", "Fashion" (used for the analytics dashboard)
    private int quantity;          // Number of items
    private BigDecimal price;      // Total price
    private String timestamp;      // Timestamp of order creation
}
