package de.asel.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String orderId;       // UUID der Bestellung (wichtig für die Idempotenz!)
    private String customerId;    // ID des Kunden
    private String productCategory;// z.B. "Electronics", "Fashion" (für das Analytics-Dashboard)
    private int quantity;         // Anzahl der Artikel
    private BigDecimal price;     // Gesamtpreis
    private String timestamp;     // Zeitpunkt der Bestellung
}
