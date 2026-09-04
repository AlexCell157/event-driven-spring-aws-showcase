# Apache Kafka

Kafka is the asynchronous boundary between the HTTP order endpoint and the processing pipeline. The broker is defined alongside LocalStack in [`localstack/docker-compose.yml`](../localstack/docker-compose.yml) and runs in KRaft mode, so no ZooKeeper container is required.

## Broker configuration

| Setting | Value |
| --- | --- |
| Container | `kafka_broker` |
| Image | `confluentinc/cp-kafka:latest` |
| Host listener | `localhost:9092` |
| Internal listener | `kafka:29092` |
| Mode | Combined broker and controller (KRaft) |
| Replication factors | `1` for the single-broker local setup |

Start it together with LocalStack:

```bash
docker compose -f localstack/docker-compose.yml up -d
```

## Application integration

The local profile configures the application as follows:

| Setting | Value | Role |
| --- | --- | --- |
| Bootstrap servers | `localhost:9092` | Connection to the local broker. |
| Topic | `orders-v1` | Receives serialized `OrderEvent` JSON. |
| Consumer group | `ecommerce-showcase-group` | Tracks processed offsets. |
| Producer acknowledgement | `all` | Waits for all in-sync replicas; locally this is one broker. |
| Offset reset | `earliest` | Reads retained events when the group has no committed offset. |

`OrderProducer` serializes an order to JSON and publishes it using `orderId` as the message key. Events for the same order therefore use the same partition. `OrderConsumer` receives the JSON from `orders-v1`, reserves the `OrderId` in DynamoDB to make processing idempotent, then writes the archive and analytics data.

## Delivery semantics and error handling

The pipeline provides at-least-once delivery with idempotent processing:

1. Before any business processing, the consumer reserves the `OrderId` in the `ProcessedOrders` DynamoDB table with a conditional write. A failed condition marks a duplicate, which is skipped without side effects.
2. If archiving to S3 or the analytics update fails after the reservation, the consumer deletes the reservation again and rethrows the exception.
3. The rethrown exception reaches the listener container, whose default error handler retries the delivery a limited number of times before giving up. Because the reservation was released, a redelivered message is processed as a fresh event rather than discarded as a duplicate.

Messages that cannot be deserialized are also rethrown and therefore retried; they are not silently dropped.

## Inspect the topic

Kafka creates the topic on first use when broker auto-topic creation is enabled. After the application has sent an order, list topics and consume records from the beginning:

```bash
docker exec kafka_broker kafka-topics \
  --bootstrap-server localhost:9092 --list

docker exec kafka_broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic orders-v1 \
  --from-beginning
```

Press `Ctrl+C` to stop the console consumer.

## Kubernetes connection

The Kubernetes manifest overrides `SPRING_KAFKA_BOOTSTRAP_SERVERS` with `kafka-service:9092`. A Kubernetes deployment therefore needs a separately deployed Kafka cluster and a Service named `kafka-service` that exposes port `9092`; the local Compose broker is not reachable under this cluster DNS name.