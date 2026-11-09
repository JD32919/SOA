# Servicio de Ventas

Microservicio 2 del proyecto SOA "Pedidos de una papeleria escolar".

## Responsabilidad

Ventas coordina la creacion de pedidos. No administra productos ni clientes: consulta esos servicios y conserva solamente la informacion de sus pedidos.

- Puerto: `9002`
- Base de datos: `ventas_db` (PostgreSQL)
- Java: 21
- Spring Boot: 4.1.2
- API: `http://localhost:9002/api/ventas`

## Requisitos

- Java 21
- PostgreSQL accesible en `localhost:5433` (o configurar `VENTAS_DB_URL`)
- Servicios Catalogo (9001) y Clientes (9003) encendidos para crear/cancelar pedidos.

## Base de datos

El script `src/main/resources/db/esquema-ventas.sql` crea el usuario, la base y las tablas. Ejecutarlo con `psql` como administrador. Cambia primero la contraseña de ejemplo.

La aplicacion usa `ddl-auto=validate`: no crea ni modifica tablas automaticamente.

Variables de entorno:

PowerShell:
```powershell
$env:VENTAS_DB_PASSWORD="tu_contrasena"
$env:CATALOGO_URL="http://localhost:9001"
$env:CLIENTES_URL="http://localhost:9003"
$env:CLIENTES_SOAP_URL="http://localhost:9003/ws"
```

Si el usuario o la URL de PostgreSQL son diferentes:
```powershell
$env:VENTAS_DB_USER="ventas_app"
$env:VENTAS_DB_URL="jdbc:postgresql://localhost:5433/ventas_db"
```

## Ejecutar

Desde esta carpeta:

```powershell
.\mvnw.cmd spring-boot:run
```

El servicio inicia en `http://localhost:9002`.

Pruebas unitarias:
```powershell
.\mvnw.cmd test
```

## Contrato REST

Todas las respuestas son JSON. Los errores tienen la forma `{"mensaje":"..."}`.

| Metodo | Ruta | Funcion |
|---|---|---|
| POST | `/api/ventas` | Registra un pedido |
| GET | `/api/ventas` | Lista pedidos |
| GET | `/api/ventas/{operacionId}` | Consulta un pedido por UUID |
| PUT | `/api/ventas/{operacionId}/cancelacion` | Cancela un pedido pendiente o confirmado |

### Crear pedido

`POST http://localhost:9002/api/ventas`

```json
{
  "operacionId": "11111111-1111-4111-8111-111111111111",
  "clienteId": 1,
  "lineas": [
    {"productoId": 1, "cantidad": 2},
    {"productoId": 3, "cantidad": 4}
  ]
}
```

El total se calcula usando los precios consultados al Catalogo. No se acepta un precio enviado por el consumidor.

Respuesta (ejemplo):
```json
{
  "operacionId": "11111111-1111-4111-8111-111111111111",
  "clienteId": 1,
  "total": 108.00,
  "estado": "CONFIRMADO",
  "creadoEn": "2026-11-08T19:00:00-06:00",
  "lineas": [
    {
      "productoId": 1,
      "nombreProducto": "Cuaderno profesional 100 hojas",
      "cantidad": 2,
      "precioUnitario": 38.00,
      "subtotal": 76.00
    },
    {
      "productoId": 3,
      "nombreProducto": "Boligrafo azul",
      "cantidad": 4,
      "precioUnitario": 8.00,
      "subtotal": 32.00
    }
  ]
}
```

El valor de fecha y los datos concretos dependen de la ejecucion real.

### Consultar pedido

`GET http://localhost:9002/api/ventas/11111111-1111-4111-8111-111111111111`

### Listar pedidos

`GET http://localhost:9002/api/ventas`

### Cancelar pedido

`PUT http://localhost:9002/api/ventas/11111111-1111-4111-8111-111111111111/cancelacion`

No requiere cuerpo. La cancelacion se propaga a Clientes y Catalogo con el mismo UUID. Puede cancelar un pedido en estado `PENDIENTE` antes de que termine la coordinacion o uno `CONFIRMADO`. Los proveedores guardan la cancelacion anticipada para rechazar una confirmacion tardia. Repetir la cancelacion no genera una segunda devolucion.

## Reglas de negocio e idempotencia

1. El UUID y el cliente son obligatorios.
2. El pedido necesita al menos una linea.
3. Producto y cantidad deben ser enteros positivos.
4. Un producto no puede repetirse en varias lineas; se debe sumar su cantidad.
5. El precio se obtiene del Catalogo; el cliente no puede manipular el total.
6. No se venden productos inactivos.
7. Clientes debe confirmar por SOAP que el cliente esta habilitado y tiene credito disponible.
8. El consumo se registra en Clientes por REST con el mismo UUID.
9. Catalogo reserva las existencias por REST con el mismo UUID.
10. El pedido se guarda primero como `PENDIENTE`; puede consultarse o cancelarse mientras se coordina.
11. Si el registro del consumo falla despues de reservar inventario, Ventas intenta cancelar el consumo y la reserva como compensacion.
12. Si se repite un UUID con los mismos datos, Ventas devuelve el pedido existente o reanuda uno pendiente.
13. Si se reutiliza el UUID con otro cliente o productos/cantidades diferentes, responde 409.
14. Un pedido pendiente o confirmado puede cancelarse. Uno cancelado no se puede reactivar con el mismo UUID.

## Integracion entre servicios

- Catalogo REST: `GET /api/productos/{id}`, `PUT /api/reservas/{uuid}` y `PUT /api/reservas/{uuid}/cancelacion`.
- Clientes SOAP: `POST /ws`, operacion `consultarCliente`, contrato `clientes-soap.xsd`.
- Clientes REST: `PUT /api/clientes/{id}/consumos/{uuid}` y su ruta `/cancelacion`.

La consulta SOAP transforma la solicitud JSON recibida por Ventas en un mensaje XML. El registro del consumo se realiza por REST/JSON.

Ventas no accede directamente a las tablas de Catalogo ni de Clientes. Cada servicio conserva la propiedad de sus datos.

## Codigos HTTP

| Codigo | Uso |
|---|---|
| 201 | Pedido creado |
| 200 | Consulta o cancelacion exitosa |
| 400 | UUID, cliente, lineas o JSON invalidos |
| 404 | Pedido no encontrado |
| 409 | Regla de negocio incumplida o UUID reutilizado con otros datos |
| 502 | Respuesta invalida o rechazo de un proveedor |
| 503 | Servicio proveedor no disponible o timeout |

## Pruebas obligatorias sugeridas para la defensa

- Crear pedido con cliente habilitado y existencias suficientes.
- Intentar pedido con cliente inhabilitado o credito insuficiente.
- Intentar cantidad invalida o producto inactivo.
- Apagar Catalogo o Clientes y comprobar el error de proveedor.
- Reiniciar Ventas y consultar que el pedido siga guardado.
- Repetir el mismo UUID y mismos datos: no debe duplicarse.
- Reutilizar el UUID con datos diferentes: debe responder 409.
- Cancelar un pedido en estado PENDIENTE y verificar que los proveedores rechacen una confirmacion tardia.
- Cancelar un pedido confirmado y verificar que se cancele el consumo y se libere inventario.
- Repetir la cancelacion y verificar que no se duplique la devolucion.

## Nota sobre consistencia distribuida

Las llamadas a proveedores se compensan cuando ocurre un error durante el alta. Si un proveedor permanece apagado durante la compensacion, se debe revisar el mismo UUID en Catalogo y Clientes y completar la cancelacion cuando vuelva a estar disponible. La compensacion distribuida no equivale a una transaccion ACID entre las tres bases de datos.
