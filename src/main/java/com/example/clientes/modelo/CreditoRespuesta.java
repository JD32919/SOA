/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.example.clientes.modelo;

import java.math.BigDecimal;

// si se manda monto, puedeComprar dice si alcanza el credito
public record CreditoRespuesta(
        int clienteId,
        boolean habilitado,
        BigDecimal limiteCredito,
        BigDecimal consumido,
        BigDecimal disponible,
        boolean puedeComprar,
        String motivo
        ){
}
