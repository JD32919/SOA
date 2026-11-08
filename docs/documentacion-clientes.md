# Documentacion del servicio de Clientes

## 1. Que hace

Servicio 3 del proyecto SOA. Guarda los **datos del cliente**, si esta **habilitado** para comprar,
su **limite de credito** y el **historial** de lo que ha consumido.
Ventas lo consulta antes de crear un pedido y le avisa cuando un pedido se confirma o se cancela.

| Dato | Valor |
|---|---|
| Puerto | 9003 |
| Base de datos | `clientes_db` (PostgreSQL, puerto 5433) |
| Usuario de la base | `clientes_app` (contrasena en `CLIENTES_DB_PASSWORD`) |
| REST | `http://localhost:9003/api/clientes` |
| SOAP | `http://localhost:9003/ws` (WSDL en `/ws/clientes.wsdl`) |

## 2. Estructura del codigo

```
com.example.clientes
├── configuracion/SoapConfig      publica el WSDL y valida los mensajes SOAP contra el XSD
├── controller/
│   ├── ClienteController         datos, habilitacion, limite, credito e historial
│   ├── ConsumoController         registrar / cancelar consumos (lo usa Ventas)
│   └── ManejadorErrores         convierte los errores en {"mensaje": "..."}
├── modelo/                       records de entrada (Solicitud*) y salida (*Respuesta)
├── persistencia/                 ClienteEntity (tabla clientes) y ClienteRepository (JPA)
├── servicio/
│   ├── ClienteService           validaciones y cambios del cliente (JPA)
│   └── ConsumoService           credito y consumos (JDBC con bloqueo FOR UPDATE)
└── soap/ClienteSoapEndpoint      operacion consultarCliente
```

## 3. Base de datos

El script esta en `src/main/resources/db/esquema-clientes.sql` (la aplicacion no crea tablas, solo las valida).

**clientes**: `id`, `nombre`, `email` (unico), `telefono`, `limite_credito`, `habilitado`,
`motivo_inhabilitacion`, `creado_en`.

**consumos**: `operacion_id` (UUID, llave), `cliente_id`, `monto`, `estado` (`CONFIRMADO` o `CANCELADO`),
`registrado_en`, `actualizado_en`.

Credito disponible = `limite_credito` − suma de consumos `CONFIRMADO`.

## 4. Endpoints REST

Todas las respuestas son JSON. Los errores siempre vienen como `{"mensaje": "..."}`.

| Metodo | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| GET | `/api/clientes` | – | lista de clientes |
| GET | `/api/clientes/{id}` | – | cliente |
| POST | `/api/clientes` | `{nombre, email, telefono, limiteCredito}` | cliente creado (201) |
| PUT | `/api/clientes/{id}` | `{nombre, email, telefono}` | cliente |
| PUT | `/api/clientes/{id}/habilitacion` | `{habilitado, motivo}` | cliente |
| PUT | `/api/clientes/{id}/limite` | `{limiteCredito}` | cliente |
| GET | `/api/clientes/{id}/credito?monto=` | – | situacion de credito |
| GET | `/api/clientes/{id}/historial` | – | lista de consumos |
| PUT | `/api/clientes/{id}/consumos/{operacionId}` | `{monto}` | consumo |
| PUT | `/api/clientes/{id}/consumos/{operacionId}/cancelacion` | – | consumo |

### Ejemplos

Crear un cliente:
```
POST /api/clientes
{"nombre": "Maria Ruiz", "email": "maria@ejemplo.com", "telefono": "5551112222", "limiteCredito": 1000}
```
```
201
{"id": 3, "nombre": "Maria Ruiz", "email": "maria@ejemplo.com", "telefono": "5551112222",
 "limiteCredito": 1000, "habilitado": true, "motivoInhabilitacion": null}
```

Consultar si le alcanza para un pedido de 1200:
```
GET /api/clientes/3/credito?monto=1200
```
```
200
{"clienteId": 3, "habilitado": true, "limiteCredito": 1000.00, "consumido": 0, "disponible": 1000.00,
 "puedeComprar": false, "motivo": "El monto supera el credito disponible (1000.00)"}
```

Registrar el consumo de un pedido:
```
PUT /api/clientes/3/consumos/11111111-1111-1111-1111-111111111111
{"monto": 600}
```
```
200
{"operacionId": "11111111-1111-1111-1111-111111111111", "clienteId": 3, "monto": 600.00,
 "estado": "CONFIRMADO", "registradoEn": "2026-11-08T06:57:46Z"}
```

## 5. Reglas de negocio

- El email es obligatorio, valido y no se puede repetir (se guarda en minusculas).
- El limite de credito no puede ser negativo y admite maximo 2 decimales.
- Para inhabilitar es obligatorio dar un motivo; al habilitar el motivo se borra.
- Un consumo solo se acepta si el cliente esta habilitado y el monto no supera su credito disponible.
- Bajar el limite por debajo de lo consumido no cancela nada, solo bloquea compras nuevas.
- Dos pedidos al mismo tiempo no pueden pasarse del limite: el cliente se bloquea (`FOR UPDATE`) mientras se registra.

## 6. Como lo usa Ventas (idempotencia)

Ventas usa **el mismo UUID de operacion** que manda a catalogo para reservar.

1. `GET /credito?monto=` para saber si el cliente puede comprar (opcional, no aparta nada).
2. `PUT /consumos/{uuid}` para cargar el pedido al cliente.
3. Si algo falla despues (por ejemplo, catalogo no tiene existencias), `PUT /consumos/{uuid}/cancelacion`.

| Situacion | Resultado |
|---|---|
| Registrar dos veces el mismo UUID con el mismo monto | regresa el mismo consumo, no cobra doble |
| Mismo UUID con otro monto u otro cliente | 409 "El UUID ya pertenece a otra solicitud" |
| Cancelar dos veces | regresa el consumo cancelado |
| La cancelacion llega antes que el registro | queda marcado como cancelado y el registro tardio da 409 |

## 7. Codigos de error

| Codigo | Cuando |
|---|---|
| 400 | datos invalidos, JSON roto o identificador con formato incorrecto |
| 404 | el cliente no existe |
| 409 | email repetido, cliente inhabilitado, credito insuficiente o UUID en conflicto |

## 8. SOAP

Operacion `consultarCliente`, namespace `http://ejemplo.com/clientes/soap`.

Solicitud:
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:cli="http://ejemplo.com/clientes/soap">
  <soapenv:Body>
    <cli:consultarClienteRequest>
      <cli:id>3</cli:id>
    </cli:consultarClienteRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

Respuesta: `id`, `nombre`, `email`, `habilitado`, `limiteCredito` y `disponible`.
Si el cliente no existe se regresa un SOAP Fault "No existe el cliente con identificador N".

## 9. Como probarlo

1. Crear `clientes_db` y el usuario `clientes_app`, y correr `esquema-clientes.sql`.
2. `set CLIENTES_DB_PASSWORD=tu_clave` y `.\mvnw.cmd spring-boot:run`.
3. Abrir el consumidor (`consumidor-papeleria`, puerto 5500) y usar la seccion **Clientes**,
   o probar con Postman / curl usando los ejemplos de arriba.
