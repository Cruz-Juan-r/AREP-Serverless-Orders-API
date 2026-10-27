package co.edu.escuelaing.orders.lambda.mapping;

import co.edu.escuelaing.orders.lambda.ErrorResponse;

/**
 * Failure reported to API Gateway in the non-proxy integration.
 *
 * <p>When a Lambda throws, the runtime returns {"errorMessage": ..., "errorType": ...}.
 * API Gateway chooses the HTTP status by matching a regular expression against
 * {@code errorMessage}, so the message carries the error body as JSON, e.g.
 * {"error":"ORDER_NOT_FOUND","message":"Order ORD-999 does not exist"}.
 */
public class IntegrationErrorException extends RuntimeException {

    public IntegrationErrorException(ErrorResponse error) {
        super(error.toJson());
    }
}
