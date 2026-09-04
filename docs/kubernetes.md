# Kubernetes

[`kubernetes/deployment.yml`](../kubernetes/deployment.yml) contains a Deployment for the Spring Boot application and a LoadBalancer Service. It deploys the application only; Kafka and AWS-compatible services must be available separately.

## Manifest contents

| Resource | Name | Configuration |
| --- | --- | --- |
| Deployment | `ecommerce-backend-deployment` | Two replicas with label `app: ecommerce-backend`. |
| Container | `ecommerce-backend-container` | Image `alexcell157/event-driven-spring-aws-showcase:latest`, port `8081`. |
| Service | `ecommerce-backend-service` | LoadBalancer on port `8081`, targeting port `8081` in the Pods. |

Each Pod requests `250m` CPU and `256Mi` memory, with limits of `500m` CPU and `512Mi` memory.

## Prerequisites

Before applying the manifest, ensure that:

1. The image in the manifest is available to every cluster node, or replace it with an accessible image reference.
2. A Kafka Service named `kafka-service` exposes a broker on port `9092`, because the Deployment sets `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-service:9092`.
3. The application receives all configuration values required by `AwsConfig` and `OrderConsumer`: AWS S3 and DynamoDB endpoints, AWS region, S3 bucket name, DynamoDB table names, Kafka topic name, and consumer group ID.
4. The referenced bucket and tables already exist. For a LocalStack-backed environment, provision them with [Terraform](terraform.md).

The repository provides only `application.properties` and `application-local.properties`; it does not provide an `application-prod.properties`. Although the manifest activates the `prod` profile, its required custom AWS and Kafka properties are not supplied there. Provide them with a ConfigMap and, where appropriate, a Secret before using this manifest outside the local setup.

## Deploy and monitor

Apply the manifest from the repository root:

```bash
kubectl apply -f kubernetes/deployment.yml
kubectl rollout status deployment/ecommerce-backend-deployment
kubectl get pods -l app=ecommerce-backend
kubectl get service ecommerce-backend-service
```

For a LoadBalancer implementation that supports external addresses, wait until `EXTERNAL-IP` is assigned:

```bash
kubectl get service ecommerce-backend-service --watch
```

On local Kubernetes distributions without a LoadBalancer controller, use port forwarding for a temporary check:

```bash
kubectl port-forward service/ecommerce-backend-service 8081:8081
```

Then call the order endpoint at `http://localhost:8081/api/v1/orders`.

## Operational notes

- The configured two replicas provide application-level redundancy, but the local single-broker Kafka setup described in [Kafka](kafka.md) does not provide broker redundancy.
- `AwsConfig` currently uses endpoint overrides and dummy credentials designed for LocalStack. A deployment that targets real AWS needs application configuration changes rather than only Kubernetes environment variables.