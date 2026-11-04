/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.catalogo.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ManejadorErrores{
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> manejar(ResponseStatusException error){
        
        String mensaje =error.getReason() != null
                ? error.getReason()
                : "No fue posible completa la operacion";
        return ResponseEntity
                .status(error.getStatusCode())
                .body(Map.of("mensaje", mensaje));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> manejarTipoInvalido(
            MethodArgumentTypeMismatchException error){
        
        return ResponseEntity.badRequest().body(
                Map.of("mensaje", "Un identificador no tiene un formato valido")
        );   
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarCuerpoInvalido(
            HttpMessageNotReadableException error){
        
        return ResponseEntity.badRequest().body(
                Map.of("mensaje", "El cuerpo de la solicitud no es un JSON valido")
        );   
    }
    
}
