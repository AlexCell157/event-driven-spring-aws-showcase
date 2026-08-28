package de.asel.ecommerce.service;

import tools.jackson.databind.json.JsonMapper;
import de.asel.ecommerce.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    private final JsonMapper jsonMapper; // Nutzt nativ Jackson 3

    @Value("${custom.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${custom.aws.dynamodb.analytics-table}")
    private String analyticsTable;

    @KafkaListener(topics = "${custom.kafka.topic-name}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrder(String message) {
        try {
            // 1. JSON in Objekt verwandeln
            OrderEvent event = jsonMapper.readValue(message, OrderEvent.class);
            log.info("Consuming OrderEvent. OrderId: {}, Category: {}", event.getOrderId(), event.getProductCategory());

            // 2. In S3 archivieren
            archiveToS3(event, message);

            // 3. Live-Metriken in DynamoDB updaten
            updateAnalyticsDashboard(event);

        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", message, e);
        }
    }

    private void archiveToS3(OrderEvent event, String jsonPayload) {
        String s3Key = "orders/" + event.getProductCategory() + "/" + event.getOrderId() + ".json";

        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(s3Key).build(),
                RequestBody.fromString(jsonPayload)
        );
        log.info("Successfully archived order to S3: {}", s3Key);
    }

    private void updateAnalyticsDashboard(OrderEvent event) {
        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(analyticsTable)
                .key(Map.of("ProductCategory", AttributeValue.builder().s(event.getProductCategory()).build()))
                .updateExpression("ADD TotalOrders :inc, TotalRevenue :rev")
                .expressionAttributeValues(Map.of(
                        ":inc", AttributeValue.builder().n("1").build(),
                        ":rev", AttributeValue.builder().n(event.getPrice().toString()).build()
                ))
                .build();

        dynamoDbClient.updateItem(updateRequest);
        log.info("Successfully updated DynamoDB real-time analytics for category: {}", event.getProductCategory());
    }
}
