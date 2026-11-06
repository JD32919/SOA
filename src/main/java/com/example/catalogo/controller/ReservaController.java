/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.catalogo.controller;

import com.example.catalogo.modelo.ReservaRespuesta;
import com.example.catalogo.modelo.SolicitudReserva;
import com.example.catalogo.servicio.ReservaService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = {
    "http://localhost:5500",
    "http://127.0.0.1:5500"
})

@RestController
@RequestMapping("/api/reservas")
public class ReservaController{
    private final ReservaService reservaService;
    
    public ReservaController(ReservaService reservaService){
        this.reservaService = reservaService;
    }
    @PutMapping("/{operacionId}")
    public ReservaRespuesta reservar(
            @PathVariable("operacionId") UUID operacionId,
            @RequestBody SolicitudReserva solicitud){
        
        return reservaService.reservar(operacionId, solicitud);
    }
    @PutMapping("/{operacionId}/cancelacion")
    public ReservaRespuesta cancelar(
            @PathVariable("operacionId") UUID operacionId){
        return reservaService.cancelar(operacionId);
    }
    @GetMapping("/{operacionId}")
    public ReservaRespuesta consultar(
            @PathVariable("operacionId") UUID operacionId) {
        return reservaService.consultar(operacionId);
    }
    
    
}