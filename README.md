# Base de datos del servicio catalogo

Motor: PostgreSQL (puerto 5433). Base: `catalogo_db`. Usuario: `catalogo_app`.

1. Crear usuario y base (como usuario postgres)

```sql
CREATE ROLE catalogo_app WITH LOGIN PASSWORD 'contraseña';
CREATE DATABASE catalogo_db OWNER catalogo_app;
```

2. Restaurar la base

Desde esta carpeta:

psql -U catalogo_app -p 5433 -h localhost -d catalogo_db -f catalogo_db.sql

Nota: usar el archivo sql adjunto en esta rama del proyecto.

3. Definir la variable de entorno con la contraseña.

Debe ser la misma contraseña del paso 1.

1. Desde windos, busca "Editar las variables de entorno de esta cuenta".
2. En variables de usuario, seleccionamos crear nueva.
3. En nombre va "CATALOGO_DB_PASSWORD" y en valor: la contraseña del paso 1.
4. Aceptamos y cerramos

Cierra y vuelve a abrir NetBeans y la terminal para que la tome.

4. Arrancar el servicio
Clonar la rama `catalogo` y ejecutarla (puerto 9001).