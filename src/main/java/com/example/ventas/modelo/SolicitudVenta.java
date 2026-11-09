package com.example.ventas.modelo;

import java.util.List;
import java.util.UUID;

public record SolicitudVenta(
        UUID operacionId,
        Integer clienteId,
        List<LineaVentaSolicitud> lineas
) {}
