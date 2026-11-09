# Servicio de Impresiones

Servicio de impresiones del sistema SOA. Corre en el puerto **9004** y usa la base de datos **impresiones_db** (PostgreSQL).

## Cómo ejecutarlo

1. Crear la base de datos en PostgreSQL:
   ```
   CREATE DATABASE impresiones_db;
   ```
2. Revisar usuario y contraseña en `src/main/resources/application.properties`.
3. Ejecutar:
   ```
   mvnw.cmd spring-boot:run
   ```
   También se puede abrir la carpeta en NetBeans y presionar Run.

## Endpoints

Base: `http://localhost:9004/api/impresiones`

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/` | Crea un trabajo de impresión |
| GET | `/` | Lista todos los trabajos |
| GET | `/cola` | Lista los trabajos que están en cola |
| GET | `/{id}` | Consulta un trabajo |
| PUT | `/{id}/iniciar` | Pasa de EN_COLA a IMPRIMIENDO |
| PUT | `/{id}/terminar?hojasReales=12` | Pasa a TERMINADO y calcula el costo final |
| PUT | `/{id}/cancelar` | Cancela el trabajo |

Ejemplo de POST:
```json
{"clienteId": 1, "documento": "tarea.pdf", "tipo": "BN", "hojasEstimadas": 10}
```

## Costos

- BN: $0.50 por hoja
- COLOR: $2.00 por hoja

## Códigos de respuesta

200 correcto, 201 creado, 400 datos inválidos, 404 no existe, 409 estado no permitido.
