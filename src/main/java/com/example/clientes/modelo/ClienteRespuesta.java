/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.example.clientes.modelo;

import java.math.BigDecimal;

public record ClienteRespuesta(
        int id,
        String nombre,
        String email,
        String telefono,
        BigDecimal limiteCredito,
        boolean habilitado,
        String motivoInhabilitacion
        ){
}
