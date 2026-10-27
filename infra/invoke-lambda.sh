#!/usr/bin/env bash
# Invokes the Architecture A Lambdas directly (no API Gateway) with JSON payloads,
# to observe how the Java runtime converts JSON <-> Java objects.
# Usage: ./infra/invoke-lambda.sh
set -uo pipefail
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."
mkdir -p target

invoke() {
  local fn=$1 payload=$2
  echo "### $fn <- $payload"
  aws lambda invoke --function-name "$fn" --cli-binary-format raw-in-base64-out \
    --payload "$payload" target/lambda-out.json --query '{status:StatusCode,functionError:FunctionError}' \
    --output text
  cat target/lambda-out.json
  echo; echo
}

invoke orders-create-order '{"customerId":"CUST-42","product":"MacBook Air","quantity":1}'
invoke orders-create-order '{"customerId":"CUST-100","product":"Monitor","quantity":3}'
invoke orders-get-order    '{"id":"ORD-001"}'
invoke orders-get-order    '{"id":"ORD-999"}'
invoke orders-find-orders  '{"customerId":"CUST-42"}'
echo "========== JSON -> Java conversion experiments =========="
invoke orders-create-order '{"customerId":"CUST-1","product":"Pen","quantity":1,"color":"blue"}'
invoke orders-create-order '{"CustomerId":"CUST-1","PRODUCT":"Pen","quantity":1}'
invoke orders-create-order '{"customerId":"CUST-1","product":"Pen","quantity":"2"}'
invoke orders-create-order '{"customerId":"CUST-1","product":"Pen","quantity":"dos"}'
invoke orders-get-order    '"ORD-001"'
