# Orders API — API REST serverless con AWS Lambda y API Gateway

API REST pequeña para gestionar pedidos (*orders*), construida en Java 17 + Maven y expuesta con AWS Lambda y Amazon API Gateway. El taller compara dos formas de conectar HTTP con Lambda (integración con *mapping templates* vs. integración *proxy*) y analiza dónde debe vivir cada responsabilidad.

## 1. Diseño de la API (contrato)

Se diseñó antes de escribir código Java.

### Representación de un pedido

```json
{
  "id": "ORD-001",
  "customerId": "CUST-42",
  "product": "MacBook Air",
  "quantity": 1
}
```

| Campo        | Tipo    | Regla                                   |
|--------------|---------|-----------------------------------------|
| `id`         | string  | Lo asigna el servidor (`ORD-NNN`)        |
| `customerId` | string  | Obligatorio, no vacío                   |
| `product`    | string  | Obligatorio, no vacío                   |
| `quantity`   | integer | Obligatorio, mayor que 0                |

### Formato de error (igual para todos los errores)

```json
{
  "error": "ORDER_NOT_FOUND",
  "message": "Order ORD-999 does not exist"
}
```

- `error`: código estable en MAYÚSCULAS, pensado para que lo lea un programa (`ORDER_NOT_FOUND`, `INVALID_REQUEST`, `INTERNAL_ERROR`, `ROUTE_NOT_FOUND`).
- `message`: explicación para una persona. Nunca incluye trazas de pila ni detalles internos.

### Operaciones

| Operación | Método y URL | Parámetros | Cuerpo de la petición | Éxito | Errores |
|---|---|---|---|---|---|
| Crear pedido | `POST /orders` | — | `{"customerId","product","quantity"}` | `201 Created` + pedido creado + header `Location: /orders/{id}` | `400 INVALID_REQUEST` (cuerpo ausente, JSON inválido, campos faltantes o inválidos), `500 INTERNAL_ERROR` |
| Consultar pedido | `GET /orders/{id}` | path: `id` | — | `200 OK` + pedido | `404 ORDER_NOT_FOUND`, `500 INTERNAL_ERROR` |
| Pedidos de un cliente | `GET /orders?customerId={customerId}` | query: `customerId` (obligatorio) | — | `200 OK` + arreglo de pedidos (puede ser `[]`) | `400 INVALID_REQUEST` (falta `customerId`), `500 INTERNAL_ERROR` |
| Eliminar pedido (extensión) | `DELETE /orders/{id}` | path: `id` | — | `204 No Content`, sin cuerpo | `404 ORDER_NOT_FOUND`, `500 INTERNAL_ERROR` |

Decisiones:

- Un cliente sin pedidos devuelve `200 OK` con `[]`, no `404`: la búsqueda se ejecutó bien y su resultado es una colección vacía.
- `GET /orders` sin `customerId` devuelve `400`. Listar todos los pedidos no forma parte del contrato.
- El cliente no envía `id` en el `POST`; si lo envía, la petición se rechaza con `400` (campo desconocido).

### Preguntas de diseño

**¿Cuáles son los recursos de la API?**
El recurso principal es la colección `/orders` y cada pedido individual `/orders/{id}`. El cliente (`customerId`) aparece como atributo del pedido y como criterio de búsqueda, pero no es un recurso propio de esta API.

**¿Por qué `/orders/{id}` para obtener un pedido?**
Porque el `id` identifica de forma única un elemento de la colección `/orders`. En REST la URL identifica el recurso y el método indica qué hacer con él: `GET /orders/ORD-42` lee ese pedido y `DELETE /orders/ORD-42` lo elimina, con la misma URL. Así cada pedido tiene una URL estable que se puede devolver en el header `Location`.

**¿Por qué `customerId` es un parámetro de query en la búsqueda?**
Porque no identifica un recurso, filtra una colección. `GET /orders?customerId=CUST-42` se lee como "los pedidos, filtrados por cliente". Si mañana se agregan más filtros (`product`, `status`), se combinan como más parámetros de query sin crear URLs nuevas. Una alternativa válida sería `/customers/{id}/orders`, pero eso supondría que el cliente es un recurso de esta API, y no lo es.

**¿Qué debe devolver `POST /orders` al crear un pedido?**
`201 Created`, el pedido completo con el `id` asignado por el servidor (el cliente no lo conoce antes) y el header `Location` con la URL del nuevo recurso.

**¿Qué debe devolver la API si un pedido no existe?**
`404 Not Found` con el cuerpo de error `{"error":"ORDER_NOT_FOUND","message":"Order ORD-999 does not exist"}`. Se aplica tanto a `GET` como a `DELETE`.

**¿Qué información debe contener una respuesta de error?**
Un código estable para programas (`error`), un mensaje legible para personas (`message`) y el código de estado HTTP correcto. No debe exponer trazas, nombres de clases ni detalles de infraestructura.
