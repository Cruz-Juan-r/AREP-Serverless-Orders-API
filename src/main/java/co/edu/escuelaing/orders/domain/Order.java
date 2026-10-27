package co.edu.escuelaing.orders.domain;

import java.util.Objects;

/**
 * A customer order. Immutable: once created, its values do not change.
 */
public final class Order {

    private final String id;
    private final String customerId;
    private final String product;
    private final int quantity;

    public Order(String id, String customerId, String product, int quantity) {
        this.id = id;
        this.customerId = customerId;
        this.product = product;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Order other)) {
            return false;
        }
        return quantity == other.quantity
                && Objects.equals(id, other.id)
                && Objects.equals(customerId, other.customerId)
                && Objects.equals(product, other.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerId, product, quantity);
    }

    @Override
    public String toString() {
        return "Order{id='" + id + "', customerId='" + customerId + "', product='" + product
                + "', quantity=" + quantity + '}';
    }
}
