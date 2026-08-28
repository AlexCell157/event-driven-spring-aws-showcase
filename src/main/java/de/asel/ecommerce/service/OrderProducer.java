package de.asel.ecommerce.service;

import de.asel.ecommerce.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Liest den Topic-Namen "orders-v1" aus deiner application-local.properties
    @Value("${custom.kafka.topic-name}")
    private String topicName;

    public void sendOrderEvent(OrderEvent event) {
        try {
            // 1. Objekt in JSON-String umwandeln
            String jsonMessage = objectMapper.writeValueAsString(event);

            log.info("Sending OrderEvent to Kafka. OrderId: {}, Topic: {}", event.getOrderId(), topicName);

            // 2. Event an Kafka senden. Wir nutzen die orderId als Kafka-Key!
            // Das sorgt dafür, dass alle Events derselben Bestellung im selben Kafka-Partition-Kanal landen.
            kafkaTemplate.send(topicName, event.getOrderId(), jsonMessage);

        } catch (JacksonException e) {
            log.error("Failed to serialize OrderEvent for OrderId: {}", event.getOrderId(), e);
        }
    }
}
