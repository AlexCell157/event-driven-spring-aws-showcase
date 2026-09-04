# LocalStack

LocalStack emulates the AWS services used by the application during local development. The Compose configuration in [`localstack/docker-compose.yml`](../localstack/docker-compose.yml) starts LocalStack together with Kafka; LocalStack itself exposes S3 and DynamoDB.

## Services and connection settings

| Setting | Value |
| --- | --- |
| Container | `localstack_main` |
| Image | `localstack/localstack:latest` |
| Endpoint | `http://localhost:4566` |
| Region | `eu-central-1` |
| Emulated services | S3, DynamoDB |

The local Spring profile in [`application-local.properties`](../src/main/resources/application-local.properties) points both AWS clients to this endpoint. `AwsConfig` creates clients with path-style S3 access and local dummy credentials, which are required by the AWS SDK but are not real AWS credentials.

## Start and stop

Start the local infrastructure from the repository root:

```bash
docker compose -f localstack/docker-compose.yml up -d
```

Check that LocalStack is reachable:

```bash
curl http://localhost:4566/_localstack/health
```

Stop the containers without deleting any Docker-managed data:

```bash
docker compose -f localstack/docker-compose.yml down
```

LocalStack Community keeps emulated resources in memory only. After the container is recreated, the S3 bucket and DynamoDB tables are gone and must be provisioned again with [Terraform](terraform.md).

## Provisioned resources

Terraform creates the following resources inside LocalStack:

| AWS service | Resource | Purpose |
| --- | --- | --- |
| S3 | `ecommerce-order-archive-showcase` | Stores the raw JSON for each processed order. |
| DynamoDB | `ECommerceAnalyticsDashboard` | Holds atomic order and revenue totals per `ProductCategory`. |
| DynamoDB | `ProcessedOrders` | Stores `OrderId` values for consumer deduplication. |

Provision them as described in [Terraform](terraform.md) before starting the application.

## Verify resources and data

Use `awslocal` if it is installed, or pass the LocalStack endpoint and dummy credentials to the AWS CLI:

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=eu-central-1

aws --endpoint-url=http://localhost:4566 s3 ls
aws --endpoint-url=http://localhost:4566 dynamodb list-tables
```

After submitting an order to the application, inspect the generated data:

```bash
aws --endpoint-url=http://localhost:4566 s3 ls \
  s3://ecommerce-order-archive-showcase/orders/

aws --endpoint-url=http://localhost:4566 dynamodb scan \
  --table-name ECommerceAnalyticsDashboard

aws --endpoint-url=http://localhost:4566 dynamodb scan \
  --table-name ProcessedOrders
```

## Notes

- The Compose file contains the `LOCALSTACK_AUTH_TOKEN` setting. Supply and rotate a token through secure local configuration; do not copy a token into documentation or commit a personal token.
- LocalStack is only enabled by the `local` Spring profile. Start the application with `-Dspring-boot.run.profiles=local` so its AWS endpoints resolve to `localhost:4566`.