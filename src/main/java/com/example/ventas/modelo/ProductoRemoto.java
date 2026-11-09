package com.example.ventas.modelo;

import java.math.BigDecimal;

public record ProductoRemoto(
        Integer id,
        String nombre,
        BigDecimal precio,
        Integer existencias,
        Boolean activo
) {}
