package com.example.ventas.controlador;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ManejadorErrores {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> manejar(ResponseStatusException error) {
        String mensaje = error.getReason() == null ? "No fue posible completar la operacion" : error.getReason();
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> manejarIdentificador(MethodArgumentTypeMismatchException error) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "El identificador debe tener un formato valido"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarJson(HttpMessageNotReadableException error) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "El cuerpo de la solicitud no es un JSON valido"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> manejarConflicto(DataIntegrityViolationException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("mensaje", "El UUID ya fue registrado o existe un conflicto de integridad"));
    }
}
