package co.edu.escuelaing.orders.lambda.mapping;

/**
 * Input of the find-orders Lambda: {"customerId": "CUST-42"}. API Gateway builds it from the query string.
 */
public class FindOrdersInput {

    private String customerId;

    public FindOrdersInput() {
    }

    public FindOrdersInput(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
}
