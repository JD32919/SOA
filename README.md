# Servicio de Impresiones (puerto 9004, base impresiones_db)

Proyecto independiente en la rama `impresiones`.

## Requisitos
- Java 21 o superior
- PostgreSQL con una base llamada `impresiones_db` (opcional: se puede usar H2 en memoria)
- No hace falta instalar Maven: el proyecto trae el wrapper (`mvnw` / `mvnw.cmd`)

## Ejecutar
Desde la raíz del repositorio:

**Con PostgreSQL** (ajustar usuario y contraseña en `src/main/resources/application.properties`):
```
CREATE DATABASE impresiones_db;
```
```
./mvnw spring-boot:run          # Linux / Git Bash
mvnw.cmd spring-boot:run        # Windows (cmd / PowerShell)
```

**Sin PostgreSQL** (base H2 en memoria, perfil `h2`):
```
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

**En NetBeans:** File > Open Project > carpeta del repositorio > Run (F6).
Para H2: Project Properties > Run > VM Options: `-Dspring.profiles.active=h2`.

## Endpoints (http://localhost:9004/api/impresiones)
| Método | Ruta | Qué hace |
|---|---|---|
| POST | / | Crea un trabajo: {"clienteId":1,"documento":"tarea.pdf","tipo":"BN","hojasEstimadas":10} |
| GET | / | Lista todos |
| GET | /cola | Lista los que están EN_COLA |
| GET | /{id} | Consulta uno |
| PUT | /{id}/iniciar | EN_COLA -> IMPRIMIENDO |
| PUT | /{id}/terminar?hojasReales=12 | IMPRIMIENDO -> TERMINADO, costo final con hojas reales |
| PUT | /{id}/cancelar | Cancela si no ha terminado |

Costos: BN $0.50 por hoja, COLOR $2.00 por hoja.
Códigos: 201 creado, 200 ok, 400 datos inválidos, 404 no existe, 409 estado no permitido.
