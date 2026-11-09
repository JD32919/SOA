Servicio Inventario:

Servicio inventario del proyecto final de papeleria escolar. Administra los productos de la papeleria, sus precios y existencias, y permite reservas y cancelar existencias de forma segura para que los demas servicio del proyecto,puedan confirmar su pedido sin vender mas de lo que hay.

Informacion general:

Nombre del servicio: catalogo
Puerto: 9001
Tecnologia usada: Java, Spring boot; Spring Data JPA; Spring Web Services
Base de datos: 	PostgreSQL, base catalogo_db, usuario catalogo_app, puerto 5433
Contrato REST: JSON/api
Contrato SOAP: 	WSDL en /ws/catalogo.wsdl

1. Qué hace:

Datos que administra-

Productos: nombre, precio, existencias y si está activo.
Reservas: una operación identificada por un UUID, con su estado (CONFIRMADA o CANCELADA) y las líneas reservadas (producto y cantidad).

Este servicio es el único que escribe en sus tablas, ningún otro servicio modifica directamente la base catalogo_db; todos pasan por su API/Gatway.

Reglas de negocio:

1. El nombre del producto es obligatorio, de máximo 120 caracteres y no se repite.
2. El precio debe ser mayor a cero y con máximo dos decimales.
3. Las existencias nunca pueden ser negativas.
4. Una reserva necesita al menos una línea, con producto y cantidad positivos. Un producto no puede repetirse dentro de la misma reserva.
5. Solo se reservan productos activos y con existencias suficientes. Si una línea falla, no se aplica ninguna (todo o nada).
6. Idempotencia por UUID: repetir una reserva con el mismo UUID y los mismos datos devuelve el resultado anterior sin descontar otra vez. Usar el mismo UUID con datos distintos responde 409.
7. Cancelación: cancelar una reserva confirmada devuelve las existencias. Cancelarla de nuevo no las devuelve dos veces.
8. Cancelar antes de reservar deja la operación marcada como cancelada.


2. Base de datos del servicio:

La base de datos no se crea sola.

Las instrucciones y el script para levantar la base de datos están en la rama catalogo-db de este repositorio, las intrucciones de como levantarla estan en su propio README.md antes de arrancar el servicio.

Resumen de lo que ahí se hace:

1. Crear el usuario catalogo_app y la base catalogo_db en PostgreSQL.
2. Restaurar el script catalogo_db.sql (tablas y datos de prueba).


3. Como encender el servico:

Requesitos:
    .JDK 21, este es el que se usa para todos los servicios
    .PostgreSQL 18 corriendo en el puerto 5433
    .La base "catalogo_db" ya creada 

Variable de entorno con la clave

La contraseña no está en el código, si no que el  archivo application.properties la toma de la variable CATALOGO_DB_PASSWORD, que debe tener la misma clave que se eligió para el usuario catalogo_app.

Arranque desde la terminal
    cd catalogo (poner la ruta donde se encuantra el servicio)
    .\mvnw.cmd spring-boot:run

4. Estructura del código
src/main/java/com/example/catalogo
├── CatalogoApplication.java
├── configuracion   SoapConfig: WSDL, XSD y validación SOAP
├── controller      ProductoController, ReservaController, ManejadorErrores
├── modelo          Solicitudes y respuestas (JSON)
├── persistencia    ProductoEntity y ProductoRepository (JPA)
├── servicio        ProductoService, ReservaService (reglas de negocio)
└── soap            ProductoSoapEndpoint

src/main/resources
├── application.properties
└── contratos/catalogo-soap.xsd