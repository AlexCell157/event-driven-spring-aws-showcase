# Event-Driven Cloud-Native E-Commerce Engine

A production-ready, highly scalable event-driven microservice showcase built with **Java 25**, **Spring Boot 4.1.1**, **Apache Kafka**, and **AWS Services (Amazon S3, Amazon DynamoDB)**. 

The entire cloud infrastructure is emulated locally using **LocalStack Community** and provisioned via **Terraform (Infrastructure as Code)**. The application is fully containerized and includes production-ready **Kubernetes** deployment and service manifests.

---

## Table of Contents
- [Architecture Overview](#architecture-overview)
  - [Data Flow Diagram](#data-flow-diagram)
  - [Core Architectural Concepts](#core-architectural-concepts)
- [Tech Stack & Dependencies](#tech-stack--dependencies)
- [Project Structure](#project-structure)
- [Configuration Reference](#configuration-reference)
- [Quickstart: How to Run Locally](#quickstart-how-to-run-locally)
  - [Prerequisites](#prerequisites)
  - [Step 1: Start Local Infrastructure (Kafka & LocalStack)](#step-1-start-local-infrastructure-kafka--localstack)
  - [Step 2: Provision Cloud Infrastructure (Terraform)](#step-2-provision-cloud-infrastructure-terraform)
  - [Step 3: Run the Spring Boot Application](#step-3-run-the-spring-boot-application)
  - [Step 4: Send Sample Order Requests](#step-4-send-sample-order-requests)
- [Verification & Observability](#verification--observability)
  - [Checking S3 Cold Storage Archive](#checking-s3-cold-storage-archive)
  - [Checking DynamoDB Real-Time Analytics](#checking-dynamodb-real-time-analytics)
  - [Checking DynamoDB Deduplication Tokens](#checking-dynamodb-deduplication-tokens)
  - [Testing Consumer Idempotency](#testing-consumer-idempotency)
- [Production Deployment (Kubernetes)](#production-deployment-kubernetes)

---

## Architecture Overview

This project showcases a non-blocking asynchronous architecture engineered for high-throughput e-commerce checkout workloads.

### Data Flow Diagram

```text
                                 +-------------------------------------------------------------+
                                 |                     SPRING BOOT BACKEND                     |
                                 |                                                             |
[HTTP Client / Frontend]         |   +-----------------------+     +-----------------------+   |
           |                     |   |    OrderController    |     |     OrderProducer     |   |
           | POST /api/v1/orders |-->| - Generates UUID & ts |---->| - Serializes JSON     |---+
           |                     |   | - Returns 200 OK fast |     | - Key = orderId       |   |
           |                     |   +-----------------------+     +-----------------------+   |
           v                     |                                                             |
  (Immediate Response)           +-------------------------------------------------------------+
                                                                |
                                                                | (Publish Event)
                                                                v
                                                +-------------------------------+
                                                |      Apache Kafka (KRaft)     |
                                                |       Topic: orders-v1        |
                                                +-------------------------------+
                                                                |
                                                                | (Consume Event)
                                                                v
                                 +-------------------------------------------------------------+
                                 |                        OrderConsumer                        |
                                 +-------------------------------------------------------------+
                                           |                                      |
                         (1. Idempotency Check / Lock)              (2. Business Processing)
                                           |                                      |
                                           v                                      +-------------------+
                         +-----------------------------------+                    |                   |
                         |        DynamoDB: ProcessedOrders  |                    v                   v
                         |  (attribute_not_exists(OrderId))  |          +------------------+ +-------------------+
                         |    Duplicate -> Skip processing   |          |  Amazon S3       | | DynamoDB          |
                         +-----------------------------------+          |  Raw JSON Bucket | | Analytics Table   |
                                                                        |  orders/{cat}/.. | | (Atomic ADD)      |
                                                                        +------------------+ +-------------------+
```

### Core Architectural Concepts

1. **REST API Gateway (Non-blocking Ingestion):**
   - The `OrderController` receives incoming checkout requests via `POST /api/v1/orders`.
   - Generates a unique `orderId` (UUID) and `timestamp` (ISO-8601 UTC) at the backend boundary for security and consistency.
   - Dispatches the order payload asynchronously to Kafka via `OrderProducer` and responds immediately with `HTTP 200 OK` (sub-millisecond latency).

2. **Message Broker (Apache Kafka in KRaft Mode):**
   - Operates in KRaft mode (ZooKeeper-less) for modern, lightweight clustering.
   - Ensures strict message ordering by using the `orderId` as the Kafka partition key.
   - Configured with `acks=all` on the producer side for maximum fault tolerance and data durability.

3. **Idempotent Consumer Pipeline (`OrderConsumer`):**
   - Listens to the `orders-v1` topic as part of consumer group `ecommerce-showcase-group`.
   - **Distributed Deduplication / Locking:** Uses Amazon DynamoDB conditional writes (`attribute_not_exists(OrderId)`) on table `ProcessedOrders`. If the `OrderId` already exists (e.g. on Kafka retries or redeliveries), the duplicate message is safely discarded.
   - **Cold Storage Archive (Amazon S3):** Persists the original raw JSON payload into `ecommerce-order-archive-showcase` under partitioned paths: `orders/{productCategory}/{orderId}.json`.
   - **Real-Time Analytics (Amazon DynamoDB):** Atomically updates aggregated metrics in table `ECommerceAnalyticsDashboard` using DynamoDB's `ADD TotalOrders :inc, TotalRevenue :rev` atomic update expression, eliminating race conditions under concurrent loads without distributed locks.

---

## Tech Stack & Dependencies

| Layer / Tool | Technology & Version | Purpose |
| :--- | :--- | :--- |
| **Language** | Java 25 | Latest modern Java LTS runtime |
| **Framework** | Spring Boot 4.1.1 | Application backbone, DI, and REST API |
| **Messaging** | Spring Kafka / Apache Kafka (KRaft) | Asynchronous event streaming & queueing |
| **Cloud SDK** | AWS SDK v2 (`software.amazon.awssdk`), Spring Cloud AWS 4.1.0 | S3 & DynamoDB client integrations |
| **Serialization** | Jackson 3 (`tools.jackson.databind.json.JsonMapper`) | High-performance JSON serialization |
| **Boilerplate** | Project Lombok | Auto-generated getters/setters/constructors/logging |
| **Cloud Emulation** | LocalStack Community (Docker) | Local AWS S3 & DynamoDB service emulation |
| **IaC** | Terraform & `tflocal` | Automated cloud infrastructure provisioning |
| **Container / Orchestration**| Docker Compose, Kubernetes | Container orchestration, Service & Deployment specs |

---

## Project Structure

```text
event-driven-spring-aws-showcase/
├── HELP.md                                 # Spring Boot reference links
├── README.md                               # Project documentation
├── pom.xml                                 # Maven dependencies & build configuration
├── mvnw / mvnw.cmd                         # Maven Wrapper scripts
│
├── localstack/
│   └── docker-compose.yml                  # LocalStack (S3, DynamoDB) & Kafka (KRaft)
│
├── terraform/
│   ├── main.tf                             # S3 bucket & DynamoDB tables definitions
│   └── terraform.tfstate                   # Local Terraform state tracking
│
├── kubernetes/
│   └── deployment.yml                      # Production Deployment & LoadBalancer Service
│
└── src/
    ├── main/
    │   ├── java/de/asel/ecommerce/
    │   │   ├── EventDrivenSpringAwsShowcaseApplication.java  # Spring Boot Main Entrypoint
    │   │   ├── config/
    │   │   │   └── AwsConfig.java          # S3Client & DynamoDbClient bean configuration
    │   │   ├── controller/
    │   │   │   └── OrderController.java    # REST API: POST /api/v1/orders
    │   │   ├── dto/
    │   │   │   └── OrderEvent.java         # Order data model (orderId, customerId, price, ...)
    │   │   └── service/
    │   │       ├── OrderProducer.java      # Kafka message publisher
    │   │       └── OrderConsumer.java      # Kafka consumer, idempotency guard, S3 & DynamoDB sync
    │   └── resources/
    │       ├── application.properties      # Default application properties
    │       ├── application-local.properties# Local profile configuration (LocalStack & Kafka endpoints)
    │       └── order.http                  # Ready-to-run HTTP requests for IntelliJ / REST Client
    └── test/
        └── java/de/asel/ecommerce/eventdrivenspringawsshowcase/
            └── EventDrivenSpringAwsShowcaseApplicationTests.java  # Spring Context Smoke Test
```

---

## Configuration Reference

Key configuration settings found in `src/main/resources/application-local.properties`:

| Property | Default Value | Description |
| :--- | :--- | :--- |
| `server.port` | `8081` | Web server HTTP port |
| `spring.cloud.aws.region.static` | `eu-central-1` | AWS Region (Frankfurt) |
| `spring.cloud.aws.s3.endpoint` | `http://localhost:4566` | LocalStack S3 Endpoint |
| `spring.cloud.aws.dynamodb.endpoint` | `http://localhost:4566` | LocalStack DynamoDB Endpoint |
| `custom.aws.s3.bucket-name` | `ecommerce-order-archive-showcase` | S3 bucket for order JSON backups |
| `custom.aws.dynamodb.analytics-table` | `ECommerceAnalyticsDashboard` | DynamoDB table for aggregated analytics |
| `custom.aws.dynamodb.processed-table` | `ProcessedOrders` | DynamoDB table for deduplication tokens |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Apache Kafka broker address |
| `spring.kafka.consumer.group-id` | `ecommerce-showcase-group` | Kafka consumer group identifier |
| `custom.kafka.topic-name` | `orders-v1` | Target Kafka topic for order events |

---

## Quickstart: How to Run Locally

### Prerequisites

- **Java 25** JDK installed (or use the included Maven wrapper `./mvnw`)
- **Docker** & **Docker Compose**
- **Terraform** (and optionally `terraform-local` / `tflocal`: `pip install terraform-local`)
- **AWS CLI** (and optionally `awscli-local` / `awslocal`: `pip install awscli-local`)

---

### Step 1: Start Local Infrastructure (Kafka & LocalStack)

Start the LocalStack container (AWS emulation) and Apache Kafka broker (KRaft mode) in Docker:

```bash
cd localstack
docker compose up -d
```

Verify that both containers are running and healthy:
```bash
docker ps
```
- LocalStack is accessible at `http://localhost:4566`
- Kafka broker is accessible at `localhost:9092`

---

### Step 2: Provision Cloud Infrastructure (Terraform)

Deploy the required S3 bucket and DynamoDB tables into LocalStack:

```bash
cd ../terraform
tflocal init
tflocal apply --auto-approve
```

> **Tip (Without `tflocal`):** If you use vanilla Terraform CLI, configure the AWS provider to point to `http://localhost:4566` or run:
> ```bash
> terraform init
> terraform apply -auto-approve
> ```

---

### Step 3: Run the Spring Boot Application

Run the application with the `local` profile enabled:

**Option A: Using Maven Wrapper (CLI)**
```bash
# From project root
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Option B: Using IntelliJ IDEA / Eclipse / VS Code**
- Open the project root directory.
- Add VM option or environment variable: `-Dspring.profiles.active=local` or `SPRING_PROFILES_ACTIVE=local`.
- Run `EventDrivenSpringAwsShowcaseApplication.java`.

The application starts on port `8081`.

---

### Step 4: Send Sample Order Requests

#### Option A: Using `curl`
```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-99",
    "productCategory": "Electronics",
    "quantity": 1,
    "price": 999.99
  }'
```

```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-99",
    "productCategory": "Fashion",
    "quantity": 2,
    "price": 149.50
  }'
```

#### Option B: Using IntelliJ HTTP Client (`src/main/resources/order.http`)
Open `src/main/resources/order.http` in your IDE and execute the pre-configured HTTP requests directly.

---

## Verification & Observability

Inspect the emulated AWS cloud state in LocalStack (`eu-central-1`) to verify end-to-end data processing:

### Checking S3 Cold Storage Archive

List archived order JSON files:
```bash
aws --endpoint-url=http://localhost:4566 s3 ls s3://ecommerce-order-archive-showcase/orders/Electronics/ --region eu-central-1
```
*(Or with `awslocal`: `awslocal s3 ls s3://ecommerce-order-archive-showcase/orders/Electronics/`)*

To read an archived file:
```bash
aws --endpoint-url=http://localhost:4566 s3 cp s3://ecommerce-order-archive-showcase/orders/Electronics/<ORDER_ID>.json - --region eu-central-1
```

### Checking DynamoDB Real-Time Analytics

Scan aggregated totals per category:
```bash
aws --endpoint-url=http://localhost:4566 dynamodb scan --table-name ECommerceAnalyticsDashboard --region eu-central-1
```
*(Or with `awslocal`: `awslocal dynamodb scan --table-name ECommerceAnalyticsDashboard`)*

**Example Output:**
```json
{
  "Items": [
    {
      "ProductCategory": { "S": "Electronics" },
      "TotalOrders": { "N": "1" },
      "TotalRevenue": { "N": "999.99" }
    }
  ],
  "Count": 1
}
```

### Checking DynamoDB Deduplication Tokens

Scan processed order IDs:
```bash
aws --endpoint-url=http://localhost:4566 dynamodb scan --table-name ProcessedOrders --region eu-central-1
```

### Testing Consumer Idempotency

If Kafka re-delivers a message or if an event with an identical `orderId` is published to `orders-v1`:
1. `OrderConsumer` calls `tryReserveOrderId(event.getOrderId())`.
2. DynamoDB evaluates `attribute_not_exists(OrderId)`.
3. Since the key already exists, DynamoDB throws `ConditionalCheckFailedException`.
4. The consumer logs: `Duplicate message detected! OrderId <id> has already been processed. Skipping.` and terminates processing immediately without modifying analytics or duplicating S3 objects.

---

## Production Deployment (Kubernetes)

The repository provides production-ready Kubernetes manifests located in `kubernetes/deployment.yml`.

### Key Features
- **High Availability & Redundancy:** Configured with 2 replicas across worker nodes.
- **Resource Constraints:** Defined CPU/memory requests (`250m` / `256Mi`) and limits (`500m` / `512Mi`).
- **Production Profile:** Injects `SPRING_PROFILES_ACTIVE=prod` and cluster Kafka connection `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-service:9092`.
- **Load Balancing:** Exposes the backend via a Kubernetes `LoadBalancer` Service on port `8081`.

### Apply Manifests
```bash
kubectl apply -f kubernetes/deployment.yml
```

Verify deployment and service status:
```bash
kubectl get deployments
kubectl get pods
kubectl get svc ecommerce-backend-service
```
