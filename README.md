# Base de datos del servicio impresiones

Motor: PostgreSQL (puerto 5432). Base: `impresiones_db`. Usuario: `postgres`.

1. Crear la base (como usuario postgres)

```sql
CREATE DATABASE impresiones_db;
```

2. Crear la tabla

Desde esta carpeta:

psql -U postgres -p 5432 -h localhost -d impresiones_db -f impresiones_db.sql

Nota: usar el archivo sql adjunto en esta rama del proyecto.

3. Revisar la contraseña

En `src/main/resources/application.properties` de la rama `impresiones`,
`spring.datasource.password` debe ser la contraseña de tu PostgreSQL.

4. Arrancar el servicio

Clonar la rama `impresiones` y ejecutarla (puerto 9004).