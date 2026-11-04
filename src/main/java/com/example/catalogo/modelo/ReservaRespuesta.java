/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.example.catalogo.modelo;

import java.util.List;
import java.util.UUID;

public record ReservaRespuesta(
        UUID operacionId,
        String estado,
        List<LineaReserva> lineas
        ){
}