package co.edu.escuelaing.orders.lambda.mapping;

/**
 * Input of the get-order Lambda: {"id": "ORD-001"}. API Gateway builds it from the path.
 */
public class GetOrderInput {

    private String id;

    public GetOrderInput() {
    }

    public GetOrderInput(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
