provider "aws" {
  region = "eu-central-1"

  # Erzwingt die Verwendung von Dummy-Werten, damit STS/IAM nicht abgefragt werden
  skip_requesting_account_id  = true
  skip_credentials_validation = true
  skip_metadata_api_check     = true
}

# (Der Rest deiner Ressourcen bleibt exakt gleich!)
resource "aws_s3_bucket" "order_archive_bucket" {
  bucket = "ecommerce-order-archive-showcase"
}

resource "aws_dynamodb_table" "analytics_dashboard_table" {
  name         = "ECommerceAnalyticsDashboard"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "ProductCategory"

  attribute {
    name = "ProductCategory"
    type = "S"
  }
}

resource "aws_dynamodb_table" "idempotent_processed_orders" {
  name         = "ProcessedOrders"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "OrderId"

  attribute {
    name = "OrderId"
    type = "S"
  }
}
