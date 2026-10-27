#!/usr/bin/env bash
# Builds the JAR, creates/updates the Lambda functions and imports both REST APIs.
# Usage: ./infra/deploy.sh            (requires valid AWS credentials)
# Env:   AWS_REGION (us-east-1), STAGE (dev), LAMBDA_ROLE_ARN (LabRole of AWS Academy)
set -euo pipefail

cd "$(dirname "$0")/.."
REGION=${AWS_REGION:-us-east-1}
STAGE=${STAGE:-dev}
JAR=target/orders-api.jar
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ROLE_ARN=${LAMBDA_ROLE_ARN:-arn:aws:iam::$ACCOUNT_ID:role/LabRole}
PKG=co.edu.escuelaing.orders.lambda

if [[ "${SKIP_BUILD:-false}" != "true" ]]; then
  mvn -q package
fi
# Git Bash on Windows: do not rewrite AWS arguments that look like paths (set after Maven, which needs it)
export MSYS_NO_PATHCONV=1

# deploy_function <name> <handler> [environment]
deploy_function() {
  local name=$1 handler=$2 environment=${3:-"Variables={}"}
  if aws lambda get-function --function-name "$name" --region "$REGION" >/dev/null 2>&1; then
    aws lambda update-function-code --function-name "$name" --zip-file "fileb://$JAR" \
      --region "$REGION" >/dev/null
    aws lambda wait function-updated-v2 --function-name "$name" --region "$REGION"
    aws lambda update-function-configuration --function-name "$name" --handler "$handler" \
      --environment "$environment" --region "$REGION" >/dev/null
    aws lambda wait function-updated-v2 --function-name "$name" --region "$REGION"
  else
    aws lambda create-function --function-name "$name" --runtime java17 --role "$ROLE_ARN" \
      --handler "$handler" --zip-file "fileb://$JAR" --memory-size 512 --timeout 15 \
      --environment "$environment" --region "$REGION" >/dev/null
    aws lambda wait function-active-v2 --function-name "$name" --region "$REGION"
  fi
  echo "  Lambda $name -> $handler" >&2
  aws lambda get-function --function-name "$name" --region "$REGION" \
    --query Configuration.FunctionArn --output text
}

# allow_api_gateway <function-name> <rest-api-id>
allow_api_gateway() {
  aws lambda add-permission --function-name "$1" --statement-id "apigateway-$2" \
    --action lambda:InvokeFunction --principal apigateway.amazonaws.com \
    --source-arn "arn:aws:execute-api:$REGION:$ACCOUNT_ID:$2/*" --region "$REGION" \
    >/dev/null 2>&1 || true   # already granted
}

# import_api <api-name> <generated-openapi-file>  -> prints the API id
import_api() {
  local name=$1 file=$2 id
  id=$(aws apigateway get-rest-apis --region "$REGION" \
    --query "items[?name=='$name'].id | [0]" --output text)
  if [[ "$id" == "None" || -z "$id" ]]; then
    id=$(aws apigateway import-rest-api --parameters endpointConfigurationTypes=REGIONAL \
      --body "fileb://$file" --region "$REGION" --query id --output text)
  else
    aws apigateway put-rest-api --rest-api-id "$id" --mode overwrite \
      --body "fileb://$file" --region "$REGION" >/dev/null
  fi
  aws apigateway create-deployment --rest-api-id "$id" --stage-name "$STAGE" \
    --region "$REGION" >/dev/null
  echo "$id"
}

echo "Architecture A: one Lambda per operation, non-proxy integration"
CREATE_ARN=$(deploy_function orders-create-order "$PKG.mapping.OrderFunctions::createOrder")
GET_ARN=$(deploy_function orders-get-order "$PKG.mapping.OrderFunctions::getOrder")
FIND_ARN=$(deploy_function orders-find-orders "$PKG.mapping.OrderFunctions::findOrdersByCustomer")

sed -e "s|__REGION__|$REGION|g" \
    -e "s|__CREATE_ORDER_ARN__|$CREATE_ARN|g" \
    -e "s|__GET_ORDER_ARN__|$GET_ARN|g" \
    -e "s|__FIND_ORDERS_ARN__|$FIND_ARN|g" \
    infra/openapi-mapping.yaml > target/openapi-mapping.generated.yaml
MAPPING_API=$(import_api orders-api-mapping target/openapi-mapping.generated.yaml)
for fn in orders-create-order orders-get-order orders-find-orders; do
  allow_api_gateway "$fn" "$MAPPING_API"
done

echo "Architecture B: one Lambda for the whole API, proxy integration"
PROXY_ARN=$(deploy_function orders-api-proxy "$PKG.proxy.OrdersApiHandler::handleRequest" \
  "Variables={LOG_EVENTS=true}")

sed -e "s|__REGION__|$REGION|g" -e "s|__PROXY_ARN__|$PROXY_ARN|g" \
    infra/openapi-proxy.yaml > target/openapi-proxy.generated.yaml
PROXY_API=$(import_api orders-api-proxy target/openapi-proxy.generated.yaml)
allow_api_gateway orders-api-proxy "$PROXY_API"

cat > target/api-urls.env <<EOF
MAPPING_URL=https://$MAPPING_API.execute-api.$REGION.amazonaws.com/$STAGE
PROXY_URL=https://$PROXY_API.execute-api.$REGION.amazonaws.com/$STAGE
EOF
echo
cat target/api-urls.env
