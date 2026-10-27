package co.edu.escuelaing.orders.domain;

/**
 * The input given to the application does not satisfy its rules.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
