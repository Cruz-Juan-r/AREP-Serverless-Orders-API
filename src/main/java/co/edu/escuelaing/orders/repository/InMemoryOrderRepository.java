package co.edu.escuelaing.orders.repository;

import co.edu.escuelaing.orders.domain.Order;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orders kept in memory.
 *
 * <p>In AWS Lambda this data lives only as long as the execution environment: it survives
 * between invocations of a warm instance, but it is lost on a cold start and it is NOT shared
 * between concurrent instances or between different Lambda functions. It is not persistent storage.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    /** Repository preloaded with a few known orders, so GET requests work on a fresh instance. */
    public static InMemoryOrderRepository withSampleData() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        repository.save(new Order(repository.nextId(), "CUST-42", "MacBook Air", 1));
        repository.save(new Order(repository.nextId(), "CUST-42", "Magic Mouse", 2));
        repository.save(new Order(repository.nextId(), "CUST-7", "Keyboard", 2));
        return repository;
    }

    @Override
    public String nextId() {
        return String.format("ORD-%03d", sequence.incrementAndGet());
    }

    @Override
    public void save(Order order) {
        orders.put(order.getId(), order);
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        return orders.values().stream()
                .filter(order -> order.getCustomerId().equals(customerId))
                .sorted(Comparator.comparing(Order::getId))
                .toList();
    }

    @Override
    public boolean deleteById(String id) {
        return orders.remove(id) != null;
    }
}
