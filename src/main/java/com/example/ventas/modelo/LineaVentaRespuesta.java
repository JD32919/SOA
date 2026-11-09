package com.example.ventas.modelo;

import java.math.BigDecimal;

public record LineaVentaRespuesta(
        Integer productoId,
        String nombreProducto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {}
