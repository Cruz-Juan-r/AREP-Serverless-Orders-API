#!/usr/bin/env bash
# Calls every endpoint of both deployed APIs and prints status, headers and body.
# Usage: ./infra/smoke-test.sh  (after ./infra/deploy.sh, which writes target/api-urls.env)
set -uo pipefail
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."
source target/api-urls.env

call() {
  local method=$1 url=$2 body=${3:-}
  echo "### $method ${url#https://*/}"
  if [[ -n "$body" ]]; then
    echo "$body"
    curl -s -i -X "$method" "$url" -H "Content-Type: application/json" -d "$body"
  else
    curl -s -i -X "$method" "$url"
  fi
  echo; echo
}

for base in "$MAPPING_URL" "$PROXY_URL"; do
  echo "================ $base ================"
  call GET  "$base/orders/ORD-001"
  call GET  "$base/orders/ORD-999"
  call GET  "$base/orders?customerId=CUST-42"
  call GET  "$base/orders?customerId=CUST-100"
  call GET  "$base/orders"
  call POST "$base/orders" '{"customerId":"CUST-42","product":"MacBook Air","quantity":1}'
  call POST "$base/orders" '{"customerId":"CUST-42","product":"","quantity":0}'
  call POST "$base/orders" '{not json'
  call DELETE "$base/orders/ORD-002"
  call DELETE "$base/orders/ORD-002"
done
