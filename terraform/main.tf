provider "aws" {
  region = "eu-central-1" # Frankfurt
}

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
