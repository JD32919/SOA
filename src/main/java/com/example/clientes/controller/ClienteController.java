/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.clientes.controller;

import com.example.clientes.modelo.ClienteRespuesta;
import com.example.clientes.modelo.ConsumoRespuesta;
import com.example.clientes.modelo.CreditoRespuesta;
import com.example.clientes.modelo.SolicitudCliente;
import com.example.clientes.modelo.SolicitudHabilitacion;
import com.example.clientes.modelo.SolicitudLimite;
import com.example.clientes.servicio.ClienteService;
import com.example.clientes.servicio.ConsumoService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = {
    "http://localhost:5500",
    "http://127.0.0.1:5500"
})

@RestController
@RequestMapping("/api/clientes")
public class ClienteController{
    
    private final ClienteService clienteService;
    private final ConsumoService consumoService;
    
    public ClienteController(ClienteService clienteService, ConsumoService consumoService){
        this.clienteService=clienteService;
        this.consumoService=consumoService;
    }
    
    @GetMapping
    public List<ClienteRespuesta> obtenerTodos(){
        return clienteService.obtenerTodos();
    }
    @GetMapping("/{id}")
    public ClienteRespuesta buscarPorId(@PathVariable("id") int id){
        ClienteRespuesta res = clienteService.buscarPorId(id);
        if(res == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
        }
        return res;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteRespuesta crear(@RequestBody SolicitudCliente solicitud){
        return clienteService.crear(solicitud);
    }
    @PutMapping("/{id}")
    public ClienteRespuesta actualizar(
            @PathVariable("id") int id,
            @RequestBody SolicitudCliente solicitud){
        return clienteService.actualizar(id, solicitud);
    }
    @PutMapping("/{id}/habilitacion")
    public ClienteRespuesta cambiarHabilitacion(
            @PathVariable("id") int id,
            @RequestBody SolicitudHabilitacion solicitud){
        return clienteService.cambiarHabilitacion(id, solicitud);
    }
    @PutMapping("/{id}/limite")
    public ClienteRespuesta cambiarLimite(
            @PathVariable("id") int id,
            @RequestBody SolicitudLimite solicitud){
        return clienteService.cambiarLimite(id, solicitud);
    }
    
    // ventas lo consulta antes de armar el pedido, ej: /api/clientes/1/credito?monto=250.00
    @GetMapping("/{id}/credito")
    public CreditoRespuesta consultarCredito(
            @PathVariable("id") int id,
            @RequestParam(name="monto", required=false) BigDecimal monto){
        return consumoService.consultarCredito(id, monto);
    }
    @GetMapping("/{id}/historial")
    public List<ConsumoRespuesta> historial(@PathVariable("id") int id){
        return consumoService.historial(id);
    }
    
    
}
