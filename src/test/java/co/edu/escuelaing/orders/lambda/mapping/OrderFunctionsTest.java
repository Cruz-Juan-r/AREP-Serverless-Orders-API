package co.edu.escuelaing.orders.lambda.mapping;

import co.edu.escuelaing.orders.domain.CreateOrderRequest;
import co.edu.escuelaing.orders.domain.Order;
import co.edu.escuelaing.orders.repository.InMemoryOrderRepository;
import co.edu.escuelaing.orders.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderFunctionsTest {

    // Same selection patterns as infra/openapi-mapping.yaml (integration responses).
    private static final Pattern NOT_FOUND_404 = Pattern.compile(".*\"error\":\"ORDER_NOT_FOUND\".*");
    private static final Pattern BAD_REQUEST_400 = Pattern.compile(".*\"error\":\"INVALID_REQUEST\".*");
    private static final Pattern INTERNAL_500 =
            Pattern.compile("(?s)(?!.*\"error\":\"(ORDER_NOT_FOUND|INVALID_REQUEST)\").+");

    private OrderFunctions functions;

    @BeforeEach
    void setUp() {
        functions = new OrderFunctions(new OrderService(InMemoryOrderRepository.withSampleData()));
    }

    @Test
    void createsAnOrder() {
        Order order = functions.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));

        assertEquals("ORD-004", order.getId());
    }

    @Test
    void getsAnExistingOrder() {
        assertEquals("Keyboard", functions.getOrder(new GetOrderInput("ORD-003")).getProduct());
    }

    @Test
    void findsOrdersByCustomer() {
        assertEquals(2, functions.findOrdersByCustomer(new FindOrdersInput("CUST-42")).size());
    }

    @Test
    void aMissingOrderProducesAMessageThatApiGatewayMapsTo404() {
        IntegrationErrorException error = assertThrows(IntegrationErrorException.class,
                () -> functions.getOrder(new GetOrderInput("ORD-999")));

        assertEquals("{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}",
                error.getMessage());
        assertTrue(NOT_FOUND_404.matcher(error.getMessage()).matches());
        assertFalse(BAD_REQUEST_400.matcher(error.getMessage()).matches());
    }

    @Test
    void invalidInputProducesAMessageThatApiGatewayMapsTo400() {
        IntegrationErrorException error = assertThrows(IntegrationErrorException.class,
                () -> functions.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", -1)));

        assertTrue(BAD_REQUEST_400.matcher(error.getMessage()).matches());
        assertFalse(NOT_FOUND_404.matcher(error.getMessage()).matches());
        assertFalse(INTERNAL_500.matcher(error.getMessage()).matches());
    }

    @Test
    void errorsNotProducedByTheAdapterAreMappedTo500() {
        // e.g. a timeout reported by the Lambda runtime itself
        assertTrue(INTERNAL_500.matcher("Task timed out after 15.02 seconds").matches());
    }

    @Test
    void aMissingQueryParameterProducesA400Message() {
        // The mapping template sends "" when ?customerId is absent.
        IntegrationErrorException error = assertThrows(IntegrationErrorException.class,
                () -> functions.findOrdersByCustomer(new FindOrdersInput("")));

        assertTrue(BAD_REQUEST_400.matcher(error.getMessage()).matches());
    }

    @Test
    void anUnexpectedFailureProducesAGeneric500Message() {
        OrderFunctions broken = new OrderFunctions(new OrderService(null));

        IntegrationErrorException error = assertThrows(IntegrationErrorException.class,
                () -> broken.getOrder(new GetOrderInput("ORD-001")));

        assertTrue(INTERNAL_500.matcher(error.getMessage()).matches());
        assertFalse(error.getMessage().contains("NullPointerException"));
    }
}
