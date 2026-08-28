# Event-Driven Cloud-Native E-Commerce Engine

A production-ready, highly scalable event-driven microservice showcase built with Java 25, Spring Boot 4.1.1, Apache Kafka, and AWS Services (S3, DynamoDB). The entire cloud infrastructure is emulated locally using LocalStack Community and managed via Terraform (Infrastructure as Code). The application is fully containerized and includes production-ready Kubernetes deployment manifests.

## Architecture Overview

This project demonstrates a non-blocking asynchronous architecture designed to handle high-throughput checkout loads:
1. **REST API Gateway:** A Spring Boot controller accepts incoming orders via HTTP POST /api/v1/orders, validates the request, and immediately offloads the task to Kafka (Non-blocking design).
2. **Event Streaming (Apache Kafka):** Acts as the highly available, fault-tolerant message broker running in the modern KRaft mode (ZooKeeper-less) utilizing an idempotent producer strategy with message ordering guaranteed via partition keys.
3. **Idempotent Consumer Pipeline:** A dedicated event consumer processes messages in parallel and guards against duplicate deliveries using a distributed lock strategy via Amazon DynamoDB conditional expressions (`attribute_not_exists`).
   - **Cold Storage Architecture:** Archives raw order events as JSON backups into Amazon S3 for long-term audit and analytical purposes.
   - **Real-Time Analytics:** Atomically updates live revenue dashboards inside Amazon DynamoDB using database-level increments (`ADD`) to eliminate race conditions under heavy concurrent loads.

## Tech Stack and Keywords
- **Backend:** Java 25, Spring Boot 4.1.1, Spring Kafka, Jackson 3 (JsonMapper), Lombok
- **Cloud and Emulation:** LocalStack, AWS SDK v2, Docker Compose
- **DevOps, IaC, and Orchestration:** Kubernetes (K8s Deployments/Services), Terraform, tflocal
- **Architectural Patterns:** Event-Driven Architecture (EDA), Distributed Locking, Message Ordering, Consumer Idempotency, Cloud-Native Parity, Reactive/Non-blocking API design, Horizontal Scaling

## How to Run Locally

### Prerequisites
Ensure you have Docker Desktop / Engine, Terraform, and kubectl installed on your machine.

### Step 1: Spin up the Infrastructure (AWS and Kafka)
Navigate to the localstack/ directory and fire up the Docker Compose cluster:
```bash
cd localstack
docker compose up -d
```
Note: This starts LocalStack and Apache Kafka (KRaft mode) in an isolated virtual bridge network.

### Step 2: Deploy Cloud Resources via Terraform
Navigate to the terraform/ directory to create the required S3 buckets and DynamoDB tables locally:
```bash
cd ../terraform
tflocal init
tflocal apply --auto-approve
```

### Step 3: Launch the Spring Boot Application
Open the backend/ folder in IntelliJ IDEA and run the application using the local profile. The application will start listening on port 8081.

### Step 4: Simulate a Live Checkout Event
Fire a sample order payload into your running cluster using curl:
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

### Step 5: Test Consumer Idempotency
Fire the exact same curl command a second time. The API gateway will accept the request, but the background consumer will detect the duplicate `orderId` via DynamoDB, log a warning, and safely skip processing to prevent data corruption.

## Production Deployment (Kubernetes)

For production deployment, the application is designed to run inside a cloud-managed Kubernetes cluster (e.g., AWS EKS). The configuration enables horizontal pod autoscaling (HPA) and high availability.

To apply the deployment and load balancer configurations to your cluster, run:
```bash
cd ../kubernetes
kubectl apply -f deployment.yaml
```

## Verification
Verify that the data successfully bridged into your local Frankfurt cloud instance (eu-central-1):
```bash
# Check S3 Backups
aws s3 ls s3://ecommerce-order-archive-showcase/orders/Electronics/ --profile localstack --region eu-central-1

# Scan Real-Time Analytics
aws dynamodb scan --table-name ECommerceAnalyticsDashboard --profile localstack --region eu-central-1

# Check Deduplication Tokens
aws dynamodb scan --table-name ProcessedOrders --profile localstack --region eu-central-1
```
