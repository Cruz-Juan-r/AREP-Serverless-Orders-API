package co.edu.escuelaing.orders.lambda.mapping;

import co.edu.escuelaing.orders.domain.CreateOrderRequest;
import co.edu.escuelaing.orders.domain.InvalidRequestException;
import co.edu.escuelaing.orders.domain.Order;
import co.edu.escuelaing.orders.domain.OrderNotFoundException;
import co.edu.escuelaing.orders.lambda.ErrorResponse;
import co.edu.escuelaing.orders.repository.InMemoryOrderRepository;
import co.edu.escuelaing.orders.service.OrderService;

import java.util.List;
import java.util.function.Supplier;

/**
 * Lambda entry points for Architecture A (API Gateway non-proxy integration with mapping templates).
 *
 * <p>Plain Java: no AWS classes. Each public method is the handler of one Lambda function,
 * configured as {@code co.edu.escuelaing.orders.lambda.mapping.OrderFunctions::getOrder}.
 * The Lambda runtime turns the incoming JSON into the parameter object and the returned
 * object back into JSON. API Gateway does the HTTP work: routing, building the input
 * JSON from the path/query/body and choosing the status code.
 */
public class OrderFunctions {

    // Created once per execution environment and reused while it stays warm.
    private static final OrderService DEFAULT_SERVICE =
            new OrderService(InMemoryOrderRepository.withSampleData());

    private final OrderService service;

    public OrderFunctions() {
        this(DEFAULT_SERVICE);
    }

    public OrderFunctions(OrderService service) {
        this.service = service;
    }

    public Order createOrder(CreateOrderRequest request) {
        return call(() -> service.createOrder(request));
    }

    public Order getOrder(GetOrderInput input) {
        return call(() -> service.getOrder(input == null ? null : input.getId()));
    }

    public List<Order> findOrdersByCustomer(FindOrdersInput input) {
        return call(() -> service.getOrdersByCustomer(input == null ? null : input.getCustomerId()));
    }

    private static <T> T call(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (OrderNotFoundException e) {
            throw new IntegrationErrorException(new ErrorResponse(ErrorResponse.ORDER_NOT_FOUND, e.getMessage()));
        } catch (InvalidRequestException e) {
            throw new IntegrationErrorException(new ErrorResponse(ErrorResponse.INVALID_REQUEST, e.getMessage()));
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new IntegrationErrorException(ErrorResponse.internalError());
        }
    }
}
