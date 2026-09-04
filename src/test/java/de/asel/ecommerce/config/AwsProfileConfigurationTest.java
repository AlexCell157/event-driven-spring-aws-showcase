package de.asel.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@ActiveProfiles("prod")
class AwsProfileConfigurationTest {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Test
    void createsDefaultAwsClientsOutsideTheLocalProfile() {
        assertThat(s3Client).isNotNull();
        assertThat(dynamoDbClient).isNotNull();
    }
}