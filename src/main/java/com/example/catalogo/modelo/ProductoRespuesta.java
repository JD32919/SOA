package com.example.catalogo.modelo;

import java.math.BigDecimal;

public record ProductoRespuesta(
        int id,
        String nombre,
        BigDecimal precio,
        int existencias,
        boolean activo
        ){
}