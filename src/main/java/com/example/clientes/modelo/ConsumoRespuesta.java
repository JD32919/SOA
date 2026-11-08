/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.example.clientes.modelo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsumoRespuesta(
        UUID operacionId,
        int clienteId,
        BigDecimal monto,
        String estado,
        OffsetDateTime registradoEn
        ){
}
