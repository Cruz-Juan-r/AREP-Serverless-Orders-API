package co.edu.escuelaing.orders.domain;

/**
 * Data needed to create an order. The id is not part of it: the system assigns it.
 *
 * <p>It is a mutable JavaBean (no-arg constructor + setters) on purpose, so that any
 * JSON library, including the AWS Lambda Java runtime, can populate it.
 * {@code quantity} is an {@link Integer} so that a missing value can be told apart from 0.
 */
public class CreateOrderRequest {

    private String customerId;
    private String product;
    private Integer quantity;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String customerId, String product, Integer quantity) {
        this.customerId = customerId;
        this.product = product;
        this.quantity = quantity;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
