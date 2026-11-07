# Servicio de Impresiones (puerto 9004, base impresiones_db)

Proyecto independiente: va en su propia carpeta dentro del repositorio, en la rama `impresiones`.

## Ejecutar
1. Abrir la carpeta `impresiones` en NetBeans (File > Open Project).
2. Poner usuario y contraseña de MySQL en `src/main/resources/application.properties`.
3. Run (F6). Sin MySQL: ejecutar con el perfil h2 (`spring.profiles.active=h2`).

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
