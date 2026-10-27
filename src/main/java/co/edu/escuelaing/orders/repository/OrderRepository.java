package co.edu.escuelaing.orders.repository;

import co.edu.escuelaing.orders.domain.Order;

import java.util.List;
import java.util.Optional;

/**
 * Storage of orders. The application depends on this interface, not on a concrete store,
 * so the in-memory version can later be replaced by DynamoDB or a relational database.
 */
public interface OrderRepository {

    /** Returns a new, unused order id. */
    String nextId();

    void save(Order order);

    Optional<Order> findById(String id);

    List<Order> findByCustomerId(String customerId);

    /** Removes the order; returns false if there was no order with that id. */
    boolean deleteById(String id);
}
