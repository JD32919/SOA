# Base de datos del servicio catalogo

Motor: PostgreSQL (puerto 5433). Base: `catalogo_db`. Usuario: `catalogo_app`.

## 1. Crear usuario y base (como usuario postgres)
```sql
CREATE ROLE catalogo_app WITH LOGIN PASSWORD 'ELIGE_TU_CLAVE';
CREATE DATABASE catalogo_db OWNER catalogo_app;
```

## 2. Restaurar la base
Desde esta carpeta:
```
psql -U catalogo_app -p 5433 -h localhost -d catalogo_db -f catalogo_db.sql
```

## 3. Definir la variable de entorno con la clave
Debe ser la misma clave del paso 1.

Windows (permanente):
```
setx CATALOGO_DB_PASSWORD "ELIGE_TU_CLAVE"
```
Cierra y vuelve a abrir NetBeans y la terminal para que la tome.

## 4. Arrancar el servicio
Clonar la rama `catalogo` y ejecutarla (puerto 9001).