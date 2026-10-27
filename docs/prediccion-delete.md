# Predicción: `DELETE /orders/{id}`

Escrita y confirmada en Git **antes** de implementar la operación.

Comportamiento esperado: `DELETE /orders/ORD-001` → `204 No Content` sin cuerpo; `DELETE /orders/ORD-999` → `404 Not Found` con `{"error":"ORDER_NOT_FOUND",...}`. La operación se agrega a la API final (arquitectura B, proxy).

| Pregunta | Predicción |
|---|---|
| ¿Qué clases Java cambian? | `OrderRepository` (nuevo método `deleteById`), `InMemoryOrderRepository` (su implementación), `OrderService` (nuevo `deleteOrder`) y `OrdersApiHandler` (nueva ruta). `Order`, `CreateOrderRequest` y las excepciones **no** cambian: `OrderNotFoundException` ya expresa "no existe". |
| ¿Se necesita una Lambda nueva? | **No.** En la arquitectura B una sola Lambda atiende toda la API; solo se agrega una ruta en el adaptador. (En la arquitectura A sí haría falta una cuarta Lambda, `orders-delete-order`.) |
| ¿Cambia la configuración de API Gateway? | **Sí, un poco.** Se definieron métodos explícitos en `openapi-proxy.yaml`, así que hay que agregar `delete` en `/orders/{id}` y volver a desplegar el stage. No haría falta si se hubiera usado `ANY` o `{proxy+}`, pero entonces API Gateway dejaría de documentar qué operaciones existen. |
| ¿Dónde vive la lógica de borrado? | En `OrderService.deleteOrder(id)`: valida el `id`, pide al repositorio que borre y, si no existía, lanza `OrderNotFoundException`. El repositorio solo borra y dice si había algo (`boolean`). |
| ¿Dónde se decide 204 o 404? | En el adaptador HTTP (`OrdersApiHandler`). El servicio no conoce códigos HTTP: termina normalmente (→ `204`) o lanza `OrderNotFoundException` (→ `404`, con el mismo manejo de errores que ya usa `GET`). |
| ¿Qué pruebas se agregan? | `OrderServiceTest`: borrar un pedido existente (y que luego `getOrder` falle), borrar uno inexistente lanza `OrderNotFoundException`, y que no se borren pedidos de otros clientes. `OrdersApiHandlerTest`: `DELETE` existente → `204` sin cuerpo, `DELETE` inexistente → `404` con el cuerpo de error, y un segundo `DELETE` del mismo pedido → `404`. |

Una observación sobre la semántica: `DELETE` es idempotente respecto al **estado** del servidor (después de uno o diez `DELETE`, el pedido no existe), aunque la **respuesta** cambie de `204` a `404`. Eso es compatible con HTTP y es lo que pide el enunciado.
