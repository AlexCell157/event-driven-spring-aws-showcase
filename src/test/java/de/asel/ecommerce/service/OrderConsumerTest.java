package de.asel.ecommerce.service;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderConsumerTest {

    @Test
    void rethrowsProcessingFailureSoKafkaCanRedeliverTheMessage() {
        S3Client s3Client = mock(S3Client.class);
        DynamoDbClient dynamoDbClient = mock(DynamoDbClient.class);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().build());

        OrderConsumer consumer = new OrderConsumer(
                s3Client,
                dynamoDbClient,
                JsonMapper.builder().build(),
                "order-archive",
                "analytics",
                "processed-orders");

        assertThatThrownBy(() -> consumer.consumeOrder("""
                {"orderId":"order-1","productCategory":"Electronics","quantity":1,"price":10.00}
                """))
                .isInstanceOf(S3Exception.class);

        verify(dynamoDbClient).deleteItem(any(DeleteItemRequest.class));
    }
}