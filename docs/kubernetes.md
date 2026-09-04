# Kubernetes

[`kubernetes/deployment.yml`](../kubernetes/deployment.yml) contains a Deployment for the Spring Boot application and a LoadBalancer Service. It deploys the application only; Kafka and AWS-compatible services must be available separately.

## Manifest contents

| Resource | Name | Configuration |
| --- | --- | --- |
| Deployment | `ecommerce-backend-deployment` | Two replicas with label `app: ecommerce-backend`. |
| Container | `ecommerce-backend-container` | Image `alexcell157/event-driven-spring-aws-showcase:latest`, port `8081`. |
| ServiceAccount | `ecommerce-backend` | Identity used by the Pods; bind it to an AWS IAM role when deploying to AWS. |
| Service | `ecommerce-backend-service` | LoadBalancer on port `8081`, targeting port `8081` in the Pods. |

Each Pod requests `250m` CPU and `256Mi` memory, with limits of `500m` CPU and `512Mi` memory.

## Prerequisites

Before applying the manifest, ensure that:

1. The image in the manifest is available to every cluster node, or replace it with an accessible image reference.
2. A Kafka Service named `kafka-service` exposes a broker on port `9092`, because the Deployment sets `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-service:9092`.
3. The S3 bucket and DynamoDB tables named in the Deployment already exist. For a LocalStack-backed environment, provision them with [Terraform](terraform.md).
4. When targeting AWS, bind `ecommerce-backend` to an IAM role that permits `s3:PutObject` for the archive bucket plus `dynamodb:PutItem`, `dynamodb:DeleteItem`, and `dynamodb:UpdateItem` for both tables. On EKS, use an IRSA annotation on this ServiceAccount; the AWS SDK then discovers its credentials automatically.

The Deployment supplies all application-specific resource names, region, topic, and consumer group as environment variables. Replace these values or move them to a ConfigMap for each environment. The `local` profile exclusively creates LocalStack clients with dummy credentials; the `prod` profile relies on the AWS SDK default credential provider chain, so do not configure LocalStack endpoint variables for production.

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
- The included ServiceAccount has no AWS permissions by itself. Add the cluster-specific IAM role binding before sending production traffic.