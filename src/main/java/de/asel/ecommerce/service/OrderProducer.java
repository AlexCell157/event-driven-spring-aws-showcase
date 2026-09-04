package de.asel.ecommerce.service;

import de.asel.ecommerce.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class OrderProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final String topicName;

    public OrderProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${custom.kafka.topic-name}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
    }

    public void sendOrderEvent(OrderEvent event) {
        try {
            // 1. Serialize object to JSON string
            String jsonMessage = objectMapper.writeValueAsString(event);

            log.info("Sending OrderEvent to Kafka. OrderId: {}, Topic: {}", event.getOrderId(), topicName);

            // 2. Send event to Kafka using orderId as the message key
            // This guarantees all events for the same order land in the same partition to maintain strict ordering.
            kafkaTemplate.send(topicName, event.getOrderId(), jsonMessage);

        } catch (JacksonException e) {
            log.error("Failed to serialize OrderEvent for OrderId: {}", event.getOrderId(), e);
        }
    }
}
