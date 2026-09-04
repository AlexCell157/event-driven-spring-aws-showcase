# Terraform

Terraform provisions the AWS resources that the local application needs. The definitions are in [`terraform/main.tf`](../terraform/main.tf) and target LocalStack in region `eu-central-1`.

## Managed resources

| Terraform resource | Created resource | Key schema / configuration |
| --- | --- | --- |
| `aws_s3_bucket.order_archive_bucket` | `ecommerce-order-archive-showcase` | Archive for raw order JSON. |
| `aws_dynamodb_table.analytics_dashboard_table` | `ECommerceAnalyticsDashboard` | On-demand capacity; partition key `ProductCategory` (`S`). |
| `aws_dynamodb_table.idempotent_processed_orders` | `ProcessedOrders` | On-demand capacity; partition key `OrderId` (`S`). |

The AWS provider disables account-ID, credentials, and metadata checks because the target is a local emulator rather than an AWS account.

## Prerequisites

1. Install Terraform.
2. Install [`terraform-local`](https://github.com/localstack/terraform-local), which provides the `tflocal` command.
3. Start LocalStack as described in [LocalStack](localstack.md).

For example, install the wrapper with Python's package manager:

```bash
python3 -m pip install terraform-local
```

## Initialize and apply

Run the commands from the repository root:

```bash
cd terraform
tflocal init
tflocal plan
tflocal apply -auto-approve
```

`tflocal` automatically routes AWS provider calls to LocalStack. Plain `terraform apply` is not sufficient with the repository's current provider configuration, because it has no LocalStack endpoint overrides.

## Verify the result

With `awslocal` installed:

```bash
awslocal s3 ls
awslocal dynamodb list-tables
```

Alternatively, use the AWS CLI as documented in [LocalStack](localstack.md#verify-resources-and-data). The expected tables are `ECommerceAnalyticsDashboard` and `ProcessedOrders`; the expected bucket is `ecommerce-order-archive-showcase`.

## Destroy local resources

To remove only the resources tracked by this Terraform state:

```bash
cd terraform
tflocal destroy -auto-approve
```

## State handling

Terraform stores its state in `terraform/terraform.tfstate` (with an automatic `terraform.tfstate.backup`). Always run the commands from inside the `terraform/` directory so they use this state. Do not edit state files manually; use `tflocal plan`, `apply`, and `destroy` to keep the state aligned with LocalStack.

Because LocalStack loses its resources when the container is recreated, the state can become stale after a restart. In that case, either re-apply (`tflocal apply -auto-approve`) or delete the local state files and initialize again.