/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.clientes.controller;

import com.example.clientes.modelo.ConsumoRespuesta;
import com.example.clientes.modelo.SolicitudConsumo;
import com.example.clientes.servicio.ConsumoService;
import java.util.UUID;
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
@RequestMapping("/api/clientes/{id}/consumos")
public class ConsumoController{
    private final ConsumoService consumoService;
    
    public ConsumoController(ConsumoService consumoService){
        this.consumoService = consumoService;
    }
    @PutMapping("/{operacionId}")
    public ConsumoRespuesta registrar(
            @PathVariable("id") int id,
            @PathVariable("operacionId") UUID operacionId,
            @RequestBody SolicitudConsumo solicitud){
        
        if(solicitud == null){
            return consumoService.registrar(id, operacionId, null);
        }
        return consumoService.registrar(id, operacionId, solicitud.monto());
    }
    @PutMapping("/{operacionId}/cancelacion")
    public ConsumoRespuesta cancelar(
            @PathVariable("id") int id,
            @PathVariable("operacionId") UUID operacionId){
        return consumoService.cancelar(id, operacionId);
    }
}
