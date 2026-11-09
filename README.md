# Consumidor de la papeleria

Pagina web (HTML + JavaScript, sin frameworks) que consume los servicios del proyecto SOA.
Corre en el puerto **5500** y llama a cada servicio por REST.

| Servicio | Puerto | Que se puede hacer desde la pagina |
|---|---|---|
| Catalogo e inventario | 9001 | ver, consultar y crear productos, cambiar existencias, reservar / cancelar / consultar reservas |
| Clientes | 9003 | ver, consultar y crear clientes, habilitar / inhabilitar, cambiar limite, ver credito e historial, registrar / cancelar consumos |
| Impresiones | 9004 | ver trabajos y la cola, crear trabajos, consultar, iniciar, terminar (con hojas reales) y cancelar |

## Encender los servicios

Cada servicio se enciende en su propia terminal (cambiar la ruta por la de cada uno).

Catalogo
```
cd C:\Proyectos_SOA\catalogo
.\mvnw.cmd spring-boot:run
```

Clientes (antes definir la contrasena de la base)
```
cd C:\Proyectos_SOA\clientes
set CLIENTES_DB_PASSWORD=tu_clave
.\mvnw.cmd spring-boot:run
```

Impresiones
```
cd C:\Proyectos_SOA\impresiones
.\mvnw.cmd spring-boot:run
```

Consumidor-papeleria
```
cd C:\Proyectos_SOA\consumidor-papeleria
py -m http.server 5500 --bind 127.0.0.1
```

Despues abrir http://127.0.0.1:5500 en el navegador.

> Si un servicio esta apagado, su seccion muestra "No fue posible comunicarse con el servicio" y las demas siguen funcionando.

## Secciones de Clientes

- **Clientes registrados**: tabla con todos los clientes. Con un ID se puede consultar el cliente, su credito (limite, consumido, disponible) o su historial de consumos.
- **Crear cliente**: nombre, email, telefono (opcional) y limite de credito.
- **Habilitacion y limite**: habilitar o inhabilitar (al inhabilitar el motivo es obligatorio) y cambiar el limite de credito.
- **Consumos**: simula lo que hara Ventas al armar un pedido. Se registra un monto con un UUID de operacion y se puede cancelar con el mismo UUID. Si el cliente esta inhabilitado o el monto supera su credito disponible, el servicio lo rechaza.

## Secciones de Impresiones

- **Trabajos de impresion**: tabla con todos los trabajos (boton "Ver todos") o solo los que estan en cola (boton "Ver cola").
- **Crear trabajo de impresion**: ID del cliente, documento, tipo (blanco y negro o color) y hojas estimadas. El servicio calcula el costo estimado (BN $0.50 por hoja, color $2.00 por hoja).
- **Consultar y avanzar un trabajo**: con el ID del trabajo se puede consultar, iniciar, terminar (indicando las hojas reales usadas, con lo que se recalcula el costo final) o cancelar.
  
## Archivos

- `index.html`: estructura de la pagina y estilos.
- `app.js`: llamadas a los servicios (`API_CATALOGO`, `API_CLIENTES`), validaciones de los formularios y como se muestran las respuestas.
