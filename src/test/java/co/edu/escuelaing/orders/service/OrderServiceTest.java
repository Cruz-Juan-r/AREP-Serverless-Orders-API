package co.edu.escuelaing.orders.service;

import co.edu.escuelaing.orders.domain.CreateOrderRequest;
import co.edu.escuelaing.orders.domain.InvalidRequestException;
import co.edu.escuelaing.orders.domain.Order;
import co.edu.escuelaing.orders.domain.OrderNotFoundException;
import co.edu.escuelaing.orders.repository.InMemoryOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderServiceTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(new InMemoryOrderRepository());
    }

    @Test
    void createsAValidOrderAndAssignsAnId() {
        Order order = service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));

        assertNotNull(order.getId());
        assertEquals("CUST-42", order.getCustomerId());
        assertEquals("MacBook Air", order.getProduct());
        assertEquals(1, order.getQuantity());
    }

    @Test
    void assignsDifferentIdsToDifferentOrders() {
        Order first = service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));
        Order second = service.createOrder(new CreateOrderRequest("CUST-42", "Magic Mouse", 2));

        assertEquals("ORD-001", first.getId());
        assertEquals("ORD-002", second.getId());
    }

    @Test
    void retrievesAnExistingOrder() {
        Order created = service.createOrder(new CreateOrderRequest("CUST-7", "Keyboard", 2));

        assertEquals(created, service.getOrder(created.getId()));
    }

    @Test
    void failsWhenTheOrderDoesNotExist() {
        OrderNotFoundException error = assertThrows(OrderNotFoundException.class,
                () -> service.getOrder("ORD-999"));

        assertEquals("Order ORD-999 does not exist", error.getMessage());
        assertEquals("ORD-999", error.getOrderId());
    }

    @Test
    void findsOnlyTheOrdersOfTheGivenCustomer() {
        service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));
        service.createOrder(new CreateOrderRequest("CUST-7", "Keyboard", 2));
        service.createOrder(new CreateOrderRequest("CUST-42", "Magic Mouse", 3));

        List<Order> orders = service.getOrdersByCustomer("CUST-42");

        assertEquals(2, orders.size());
        assertTrue(orders.stream().allMatch(o -> o.getCustomerId().equals("CUST-42")));
    }

    @Test
    void returnsAnEmptyListForACustomerWithoutOrders() {
        assertEquals(List.of(), service.getOrdersByCustomer("CUST-100"));
    }

    @Test
    void rejectsAMissingRequest() {
        assertThrows(InvalidRequestException.class, () -> service.createOrder(null));
    }

    @Test
    void rejectsMissingFieldsAndReportsAllOfThem() {
        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> service.createOrder(new CreateOrderRequest(" ", null, null)));

        assertEquals("customerId is required; product is required; quantity is required",
                error.getMessage());
    }

    @Test
    void rejectsANonPositiveQuantity() {
        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 0)));

        assertEquals("quantity must be greater than 0", error.getMessage());
    }

    @Test
    void rejectsASearchWithoutCustomer() {
        assertThrows(InvalidRequestException.class, () -> service.getOrdersByCustomer(""));
        assertThrows(InvalidRequestException.class, () -> service.getOrdersByCustomer(null));
    }

    @Test
    void sampleDataContainsKnownOrders() {
        OrderService withSamples = new OrderService(InMemoryOrderRepository.withSampleData());

        assertEquals("MacBook Air", withSamples.getOrder("ORD-001").getProduct());
        assertEquals(2, withSamples.getOrdersByCustomer("CUST-42").size());
    }
}
