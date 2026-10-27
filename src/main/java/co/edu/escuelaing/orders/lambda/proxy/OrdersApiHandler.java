package co.edu.escuelaing.orders.lambda.proxy;

import co.edu.escuelaing.orders.domain.CreateOrderRequest;
import co.edu.escuelaing.orders.domain.InvalidRequestException;
import co.edu.escuelaing.orders.domain.Order;
import co.edu.escuelaing.orders.domain.OrderNotFoundException;
import co.edu.escuelaing.orders.lambda.ErrorResponse;
import co.edu.escuelaing.orders.repository.InMemoryOrderRepository;
import co.edu.escuelaing.orders.service.OrderService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP adapter for Architecture B (API Gateway Lambda proxy integration).
 *
 * <p>One Lambda serves the whole Orders API. API Gateway forwards the complete HTTP request
 * as an {@link APIGatewayProxyRequestEvent}; this class translates it into a call to
 * {@link OrderService} and translates the result (or the domain exception) into an HTTP
 * response. HTTP knowledge lives here; business rules stay in the service.
 */
public class OrdersApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // Created once per execution environment and reused while it stays warm.
    private static final OrderService DEFAULT_SERVICE =
            new OrderService(InMemoryOrderRepository.withSampleData());

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final OrderService service;
    private final boolean logEvents;

    public OrdersApiHandler() {
        this(DEFAULT_SERVICE, "true".equalsIgnoreCase(System.getenv("LOG_EVENTS")));
    }

    public OrdersApiHandler(OrderService service) {
        this(service, false);
    }

    private OrdersApiHandler(OrderService service, boolean logEvents) {
        this.service = service;
        this.logEvents = logEvents;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        if (logEvents) {
            System.out.println("Proxy event: " + toJson(event));
        }
        try {
            return route(event);
        } catch (InvalidRequestException e) {
            return error(400, new ErrorResponse(ErrorResponse.INVALID_REQUEST, e.getMessage()));
        } catch (OrderNotFoundException e) {
            return error(404, new ErrorResponse(ErrorResponse.ORDER_NOT_FOUND, e.getMessage()));
        } catch (RuntimeException e) {
            // Details go to CloudWatch, never to the client.
            e.printStackTrace();
            return error(500, ErrorResponse.internalError());
        }
    }

    private APIGatewayProxyResponseEvent route(APIGatewayProxyRequestEvent event) {
        String route = event.getHttpMethod() + " " + event.getResource();
        return switch (route) {
            case "POST /orders" -> createOrder(event);
            case "GET /orders" -> findOrdersByCustomer(event);
            case "GET /orders/{id}" -> getOrder(event);
            case "DELETE /orders/{id}" -> deleteOrder(event);
            default -> error(404, new ErrorResponse(ErrorResponse.ROUTE_NOT_FOUND, "No route for " + route));
        };
    }

    private APIGatewayProxyResponseEvent createOrder(APIGatewayProxyRequestEvent event) {
        Order order = service.createOrder(readBody(event, CreateOrderRequest.class));
        return json(201, order, "Location", locationOf(event, order));
    }

    private APIGatewayProxyResponseEvent getOrder(APIGatewayProxyRequestEvent event) {
        return json(200, service.getOrder(pathParameter(event, "id")));
    }

    private APIGatewayProxyResponseEvent deleteOrder(APIGatewayProxyRequestEvent event) {
        service.deleteOrder(pathParameter(event, "id"));
        return new APIGatewayProxyResponseEvent().withStatusCode(204);
    }

    private APIGatewayProxyResponseEvent findOrdersByCustomer(APIGatewayProxyRequestEvent event) {
        return json(200, service.getOrdersByCustomer(queryParameter(event, "customerId")));
    }

    private static <T> T readBody(APIGatewayProxyRequestEvent event, Class<T> type) {
        String body = event.getBody();
        if (body == null || body.isBlank()) {
            throw new InvalidRequestException("Request body is required");
        }
        if (Boolean.TRUE.equals(event.getIsBase64Encoded())) {
            body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
        }
        try {
            return JSON.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException("Request body is not a valid order JSON document");
        }
    }

    private static String pathParameter(APIGatewayProxyRequestEvent event, String name) {
        Map<String, String> parameters = event.getPathParameters();
        return parameters == null ? null : parameters.get(name);
    }

    private static String queryParameter(APIGatewayProxyRequestEvent event, String name) {
        Map<String, String> parameters = event.getQueryStringParameters();
        return parameters == null ? null : parameters.get(name);
    }

    /** URL of the new order, including the stage prefix when called through API Gateway (/dev/orders/ORD-004). */
    private static String locationOf(APIGatewayProxyRequestEvent event, Order order) {
        String collection = event.getRequestContext() != null && event.getRequestContext().getPath() != null
                ? event.getRequestContext().getPath()
                : event.getPath();
        return collection + "/" + order.getId();
    }

    private static APIGatewayProxyResponseEvent json(int status, Object body, String... extraHeaders) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(headers(extraHeaders))
                .withBody(toJson(body));
    }

    private static APIGatewayProxyResponseEvent error(int status, ErrorResponse error) {
        return json(status, error);
    }

    /** Content-Type plus optional name/value pairs. */
    private static Map<String, String> headers(String... namesAndValues) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        for (int i = 0; i + 1 < namesAndValues.length; i += 2) {
            headers.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return headers;
    }

    private static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize response", e);
        }
    }
}
