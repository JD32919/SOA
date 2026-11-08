# Servicio de Clientes

Puerto `9003`, base `clientes_db`. Datos del cliente, habilitacion, limite de credito e historial de consumos.

## Como levantarlo

1. Crear la base y el usuario `clientes_app`, luego correr `src/main/resources/db/esquema-clientes.sql`
2. `CLIENTES_DB_PASSWORD=... ./mvnw spring-boot:run`

## REST `/api/clientes`

| Metodo | Ruta | Que hace |
|---|---|---|
| GET | `/api/clientes` | lista clientes |
| GET | `/api/clientes/{id}` | un cliente |
| POST | `/api/clientes` | crea `{nombre, email, telefono, limiteCredito}` |
| PUT | `/api/clientes/{id}` | cambia nombre, email, telefono |
| PUT | `/api/clientes/{id}/habilitacion` | `{habilitado, motivo}` (motivo obligatorio al inhabilitar) |
| PUT | `/api/clientes/{id}/limite` | `{limiteCredito}` |
| GET | `/api/clientes/{id}/credito?monto=` | credito disponible y si alcanza para ese monto |
| GET | `/api/clientes/{id}/historial` | consumos del cliente |
| PUT | `/api/clientes/{id}/consumos/{operacionId}` | Ventas registra el pedido `{monto}` |
| PUT | `/api/clientes/{id}/consumos/{operacionId}/cancelacion` | Ventas cancela el pedido |

Los consumos usan el mismo UUID de operacion que Ventas manda a catalogo. Repetir la llamada con
los mismos datos regresa lo mismo; si la cancelacion llega primero, el registro posterior da 409.

Documentacion completa: [docs/documentacion-clientes.md](docs/documentacion-clientes.md)

## SOAP

`POST /ws`, WSDL en `/ws/clientes.wsdl`, operacion `consultarCliente` (namespace `http://ejemplo.com/clientes/soap`).
