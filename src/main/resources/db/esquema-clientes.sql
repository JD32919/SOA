-- Esquema del servicio de Clientes (clientes_db).
-- Se ejecuta a mano: la aplicacion usa ddl-auto=validate.

CREATE TABLE IF NOT EXISTS public.clientes (
    id                     SERIAL PRIMARY KEY,
    nombre                 VARCHAR(120)  NOT NULL,
    email                  VARCHAR(160)  NOT NULL UNIQUE,
    telefono               VARCHAR(20),
    limite_credito         NUMERIC(10,2) NOT NULL CHECK (limite_credito >= 0),
    habilitado             BOOLEAN       NOT NULL DEFAULT TRUE,
    motivo_inhabilitacion  VARCHAR(200),
    creado_en              TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Historial de consumos. Cada operacion de Ventas usa su UUID como llave,
-- asi registrar o cancelar dos veces la misma operacion es idempotente.
CREATE TABLE IF NOT EXISTS public.consumos (
    operacion_id    UUID          PRIMARY KEY,
    cliente_id      INTEGER       NOT NULL REFERENCES public.clientes(id),
    monto           NUMERIC(10,2) NOT NULL CHECK (monto >= 0),
    estado          VARCHAR(20)   NOT NULL CHECK (estado IN ('CONFIRMADO', 'CANCELADO')),
    registrado_en   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_consumos_cliente
    ON public.consumos (cliente_id, registrado_en DESC);

-- Usuario de la aplicacion (cambia la contrasena antes de usarlo).
-- CREATE USER clientes_app WITH PASSWORD 'cambiar';
GRANT SELECT, INSERT, UPDATE ON public.clientes, public.consumos TO clientes_app;
GRANT USAGE, SELECT ON SEQUENCE public.clientes_id_seq TO clientes_app;

-- Datos de prueba.
INSERT INTO public.clientes (nombre, email, telefono, limite_credito)
VALUES ('Ana Lopez',  'ana@ejemplo.com',  '5551234567', 5000.00),
       ('Luis Perez', 'luis@ejemplo.com', '5557654321', 1500.00)
ON CONFLICT (email) DO NOTHING;
