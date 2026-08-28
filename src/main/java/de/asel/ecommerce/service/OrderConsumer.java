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
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
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
    private final JsonMapper jsonMapper;

    @Value("${custom.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${custom.aws.dynamodb.analytics-table}")
    private String analyticsTable;

    // Liest den Namen der zweiten Tabelle "ProcessedOrders" aus den Properties
    @Value("${custom.aws.dynamodb.processed-table}")
    private String processedTable;

    @KafkaListener(topics = "${custom.kafka.topic-name}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrder(String message) {
        try {
            OrderEvent event = jsonMapper.readValue(message, OrderEvent.class);
            log.info("Received OrderEvent. Testing idempotency for OrderId: {}", event.getOrderId());

            // 1. SCHUTZSCHILD: Versuche die OrderId in ProcessedOrders zu loggen
            if (!tryReserveOrderId(event.getOrderId())) {
                log.warn("Duplicate message detected! OrderId {} has already been processed. Skipping.", event.getOrderId());
                return; // Nachricht wird ohne weitere Aktion verworfen
            }

            // 2. Eigentliche Verarbeitung (wird nur ausgeführt, wenn es kein Duplikat war)
            archiveToS3(event, message);
            updateAnalyticsDashboard(event);

        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", message, e);
        }
    }

    private boolean tryReserveOrderId(String orderId) {
        try {
            // PutItemRequest mit einer Bedingung (ConditionExpression)
            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(processedTable)
                    .item(Map.of("OrderId", AttributeValue.builder().s(orderId).build()))
                    // "Nur einfügen, wenn die OrderId noch NICHT existiert"
                    .conditionExpression("attribute_not_exists(OrderId)")
                    .build();

            dynamoDbClient.putItem(putRequest);
            log.info("OrderId {} successfully locked in DynamoDB.", orderId);
            return true;
        } catch (ConditionalCheckFailedException e) {
            // Diese Exception fliegt automatisch, wenn die OrderId schon existiert
            return false;
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
