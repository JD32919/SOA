package com.example.ventas.modelo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record VentaRespuesta(
        UUID operacionId,
        Integer clienteId,
        BigDecimal total,
        String estado,
        OffsetDateTime creadoEn,
        List<LineaVentaRespuesta> lineas
) {}
