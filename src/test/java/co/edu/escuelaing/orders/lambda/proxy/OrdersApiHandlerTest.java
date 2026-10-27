package co.edu.escuelaing.orders.lambda.proxy;

import co.edu.escuelaing.orders.repository.InMemoryOrderRepository;
import co.edu.escuelaing.orders.service.OrderService;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdersApiHandlerTest {

    private OrdersApiHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrdersApiHandler(new OrderService(InMemoryOrderRepository.withSampleData()));
    }

    @Test
    void getExistingOrderReturns200() {
        APIGatewayProxyResponseEvent response = handle(request("GET", "/orders/{id}")
                .withPathParameters(Map.of("id", "ORD-003")));

        assertEquals(200, response.getStatusCode());
        assertEquals("{\"id\":\"ORD-003\",\"customerId\":\"CUST-7\",\"product\":\"Keyboard\",\"quantity\":2}",
                response.getBody());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    void getMissingOrderReturns404WithErrorBody() {
        APIGatewayProxyResponseEvent response = handle(request("GET", "/orders/{id}")
                .withPathParameters(Map.of("id", "ORD-999")));

        assertEquals(404, response.getStatusCode());
        assertEquals("{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}",
                response.getBody());
    }

    @Test
    void postValidOrderReturns201WithLocation() {
        APIGatewayProxyRequestEvent request = request("POST", "/orders")
                .withPath("/orders")
                .withBody("{\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}");
        request.setRequestContext(new APIGatewayProxyRequestEvent.ProxyRequestContext().withPath("/dev/orders"));

        APIGatewayProxyResponseEvent response = handle(request);

        assertEquals(201, response.getStatusCode());
        assertEquals("{\"id\":\"ORD-004\",\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}",
                response.getBody());
        assertEquals("/dev/orders/ORD-004", response.getHeaders().get("Location"));
    }

    @Test
    void postedOrderCanBeReadBack() {
        handle(request("POST", "/orders").withPath("/orders")
                .withBody("{\"customerId\":\"CUST-9\",\"product\":\"Monitor\",\"quantity\":3}"));

        APIGatewayProxyResponseEvent response = handle(request("GET", "/orders/{id}")
                .withPathParameters(Map.of("id", "ORD-004")));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Monitor"));
    }

    @Test
    void postWithInvalidFieldsReturns400() {
        APIGatewayProxyResponseEvent response = handle(request("POST", "/orders")
                .withBody("{\"customerId\":\"CUST-42\",\"product\":\"\",\"quantity\":0}"));

        assertEquals(400, response.getStatusCode());
        assertEquals("{\"error\":\"INVALID_REQUEST\",\"message\":\"product is required; quantity must be greater than 0\"}",
                response.getBody());
    }

    @Test
    void postWithoutBodyReturns400() {
        assertEquals(400, handle(request("POST", "/orders")).getStatusCode());
    }

    @Test
    void postWithMalformedJsonReturns400() {
        APIGatewayProxyResponseEvent response = handle(request("POST", "/orders").withBody("{not json"));

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("INVALID_REQUEST"));
    }

    @Test
    void postWithClientGeneratedIdReturns400() {
        APIGatewayProxyResponseEvent response = handle(request("POST", "/orders")
                .withBody("{\"id\":\"ORD-1\",\"customerId\":\"CUST-42\",\"product\":\"Mouse\",\"quantity\":1}"));

        assertEquals(400, response.getStatusCode());
    }

    @Test
    void findByCustomerReturns200WithList() {
        APIGatewayProxyResponseEvent response = handle(request("GET", "/orders")
                .withQueryStringParameters(Map.of("customerId", "CUST-7")));

        assertEquals(200, response.getStatusCode());
        assertEquals("[{\"id\":\"ORD-003\",\"customerId\":\"CUST-7\",\"product\":\"Keyboard\",\"quantity\":2}]",
                response.getBody());
    }

    @Test
    void findByCustomerWithoutOrdersReturnsEmptyList() {
        APIGatewayProxyResponseEvent response = handle(request("GET", "/orders")
                .withQueryStringParameters(Map.of("customerId", "CUST-100")));

        assertEquals(200, response.getStatusCode());
        assertEquals("[]", response.getBody());
    }

    @Test
    void findWithoutCustomerIdReturns400() {
        APIGatewayProxyResponseEvent response = handle(request("GET", "/orders"));

        assertEquals(400, response.getStatusCode());
        assertEquals("{\"error\":\"INVALID_REQUEST\",\"message\":\"customerId is required\"}", response.getBody());
    }

    @Test
    void deleteExistingOrderReturns204WithoutBody() {
        APIGatewayProxyResponseEvent response = handle(request("DELETE", "/orders/{id}")
                .withPathParameters(Map.of("id", "ORD-001")));

        assertEquals(204, response.getStatusCode());
        assertNull(response.getBody());
        assertEquals(404, handle(request("GET", "/orders/{id}")
                .withPathParameters(Map.of("id", "ORD-001"))).getStatusCode());
    }

    @Test
    void deleteMissingOrderReturns404() {
        APIGatewayProxyResponseEvent response = handle(request("DELETE", "/orders/{id}")
                .withPathParameters(Map.of("id", "ORD-999")));

        assertEquals(404, response.getStatusCode());
        assertEquals("{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}",
                response.getBody());
    }

    @Test
    void deletingTheSameOrderTwiceReturns404TheSecondTime() {
        APIGatewayProxyRequestEvent delete = request("DELETE", "/orders/{id}")
                .withPathParameters(Map.of("id", "ORD-002"));

        assertEquals(204, handle(delete).getStatusCode());
        assertEquals(404, handle(delete).getStatusCode());
    }

    @Test
    void unknownRouteReturns404() {
        APIGatewayProxyResponseEvent response = handle(request("PATCH", "/orders/{id}"));

        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("ROUTE_NOT_FOUND"));
    }

    @Test
    void unexpectedFailureReturns500WithoutInternalDetails() {
        OrdersApiHandler broken = new OrdersApiHandler(new OrderService(null));

        APIGatewayProxyResponseEvent response = broken.handleRequest(request("GET", "/orders/{id}")
                .withPathParameters(Map.of("id", "ORD-001")), null);

        assertEquals(500, response.getStatusCode());
        assertEquals("{\"error\":\"INTERNAL_ERROR\",\"message\":\"An unexpected error occurred\"}",
                response.getBody());
    }

    private APIGatewayProxyResponseEvent handle(APIGatewayProxyRequestEvent request) {
        return handler.handleRequest(request, null);
    }

    private static APIGatewayProxyRequestEvent request(String method, String resource) {
        return new APIGatewayProxyRequestEvent().withHttpMethod(method).withResource(resource);
    }
}
