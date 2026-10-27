package co.edu.escuelaing.orders.service;

import co.edu.escuelaing.orders.domain.CreateOrderRequest;
import co.edu.escuelaing.orders.domain.InvalidRequestException;
import co.edu.escuelaing.orders.domain.Order;
import co.edu.escuelaing.orders.domain.OrderNotFoundException;
import co.edu.escuelaing.orders.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Application logic for orders. Knows about orders, customers, products and quantities;
 * knows nothing about HTTP, API Gateway or AWS Lambda.
 */
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order createOrder(CreateOrderRequest request) {
        validate(request);
        Order order = new Order(
                repository.nextId(),
                request.getCustomerId().trim(),
                request.getProduct().trim(),
                request.getQuantity());
        repository.save(order);
        return order;
    }

    public Order getOrder(String id) {
        requireText(id, "id");
        return repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> getOrdersByCustomer(String customerId) {
        requireText(customerId, "customerId");
        return repository.findByCustomerId(customerId);
    }

    private static void validate(CreateOrderRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Order data is required");
        }
        List<String> problems = new ArrayList<>();
        if (isBlank(request.getCustomerId())) {
            problems.add("customerId is required");
        }
        if (isBlank(request.getProduct())) {
            problems.add("product is required");
        }
        if (request.getQuantity() == null) {
            problems.add("quantity is required");
        } else if (request.getQuantity() <= 0) {
            problems.add("quantity must be greater than 0");
        }
        if (!problems.isEmpty()) {
            throw new InvalidRequestException(String.join("; ", problems));
        }
    }

    private static void requireText(String value, String name) {
        if (isBlank(value)) {
            throw new InvalidRequestException(name + " is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
