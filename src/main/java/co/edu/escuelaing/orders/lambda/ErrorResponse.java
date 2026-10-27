package co.edu.escuelaing.orders.lambda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The JSON error body defined in the API contract: {"error": "...", "message": "..."}.
 * Shared by both Lambda adapters so that both architectures return the same errors.
 */
public final class ErrorResponse {

    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String ROUTE_NOT_FOUND = "ROUTE_NOT_FOUND";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String error;
    private final String message;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public static ErrorResponse internalError() {
        return new ErrorResponse(INTERNAL_ERROR, "An unexpected error occurred");
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize error response", e);
        }
    }
}
