/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.catalogo.controller;

import com.example.catalogo.modelo.ProductoRespuesta;
import com.example.catalogo.modelo.SolicitudExistencias;
import com.example.catalogo.modelo.SolicitudProducto;
import com.example.catalogo.servicio.ProductoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/productos")
public class ProductoController{
    
    private final ProductoService productoService;
    
    public ProductoController(ProductoService productoService){
        this.productoService=productoService;
    }
    
    @GetMapping
    public List<ProductoRespuesta> obtenerTodos(){
        return productoService.obtenerTodos();
    }
    @GetMapping("/{id}")
    public ProductoRespuesta buscarPorId(@PathVariable("id") int id){
        return productoService 
                .buscarPorId(id) 
                .orElseThrow(() ->new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado"
                ));   
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoRespuesta crear( @RequestBody SolicitudProducto solicitud){
        return productoService.crear(solicitud);
    }
    @PutMapping("/{id}/existencias")
    public ProductoRespuesta cambiarExistencias(
            @PathVariable("id") int id,
            @RequestBody SolicitudExistencias solicitud){
        return productoService.cambiarExistencias(id, solicitud);
    }
}