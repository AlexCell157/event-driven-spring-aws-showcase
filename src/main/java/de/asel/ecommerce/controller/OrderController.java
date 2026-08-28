package de.asel.ecommerce.controller;

import de.asel.ecommerce.dto.OrderEvent;
import de.asel.ecommerce.service.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderProducer orderProducer;

    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody OrderEvent orderRequest) {
        log.info("Received web request to place order for customer: {}", orderRequest.getCustomerId());

        // 1. IDs und Zeitstempel dynamisch im Backend generieren (Sicherheits-Best-Practice)
        orderRequest.setOrderId(UUID.randomUUID().toString());
        orderRequest.setTimestamp(Instant.now().toString());

        // 2. Asynchrones Event an Kafka übergeben
        orderProducer.sendOrderEvent(orderRequest);

        // 3. Dem Client sofort antworten (Non-blocking Architektur)
        return ResponseEntity.ok("Order submitted successfully. Order ID: " + orderRequest.getOrderId());
    }
}
