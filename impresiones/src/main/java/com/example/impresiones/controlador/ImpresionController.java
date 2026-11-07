package com.example.impresiones.controlador;

import com.example.impresiones.modelo.Impresion;
import com.example.impresiones.servicio.ImpresionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/impresiones")
@CrossOrigin(origins = {
        "http://localhost:5500",
        "http://127.0.0.1:5500",
        "http://localhost:9002"
})
public class ImpresionController {

    private final ImpresionService servicio;

    public ImpresionController(ImpresionService servicio) {
        this.servicio = servicio;
    }

    // POST /api/impresiones
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Impresion datos) {
        if (datos.getClienteId() == null || datos.getClienteId() <= 0) {
            return error(400, "El clienteId debe ser mayor que cero");
        }
        if (datos.getDocumento() == null || datos.getDocumento().isBlank()) {
            return error(400, "El documento es obligatorio");
        }
        if (datos.getTipo() == null) {
            return error(400, "El tipo debe ser BN o COLOR");
        }
        String tipo = datos.getTipo().toUpperCase();
        if (!tipo.equals("BN") && !tipo.equals("COLOR")) {
            return error(400, "El tipo debe ser BN o COLOR");
        }
        if (datos.getHojasEstimadas() <= 0) {
            return error(400, "Las hojas estimadas deben ser mayores que cero");
        }
        datos.setTipo(tipo);
        return ResponseEntity.status(201).body(servicio.crear(datos));
    }

    // GET /api/impresiones
    @GetMapping
    public List<Impresion> obtenerTodas() {
        return servicio.obtenerTodas();
    }

    // GET /api/impresiones/cola
    @GetMapping("/cola")
    public List<Impresion> obtenerCola() {
        return servicio.obtenerCola();
    }

    // GET /api/impresiones/1
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable("id") Long id) {
        Optional<Impresion> resultado = servicio.buscarPorId(id);
        if (resultado.isEmpty()) {
            return error(404, "Trabajo de impresión no encontrado");
        }
        return ResponseEntity.ok(resultado.get());
    }

    // PUT /api/impresiones/1/iniciar
    @PutMapping("/{id}/iniciar")
    public ResponseEntity<?> iniciar(@PathVariable("id") Long id) {
        Optional<Impresion> resultado = servicio.buscarPorId(id);
        if (resultado.isEmpty()) {
            return error(404, "Trabajo de impresión no encontrado");
        }
        Impresion impresion = resultado.get();
        if (!impresion.getEstado().equals("EN_COLA")) {
            return error(409, "Solo se puede iniciar un trabajo que está EN_COLA");
        }
        impresion.setEstado("IMPRIMIENDO");
        return ResponseEntity.ok(servicio.guardar(impresion));
    }

    // PUT /api/impresiones/1/terminar?hojasReales=12
    @PutMapping("/{id}/terminar")
    public ResponseEntity<?> terminar(@PathVariable("id") Long id,
                                      @RequestParam("hojasReales") int hojasReales) {
        Optional<Impresion> resultado = servicio.buscarPorId(id);
        if (resultado.isEmpty()) {
            return error(404, "Trabajo de impresión no encontrado");
        }
        if (hojasReales <= 0) {
            return error(400, "Las hojas reales deben ser mayores que cero");
        }
        Impresion impresion = resultado.get();
        if (!impresion.getEstado().equals("IMPRIMIENDO")) {
            return error(409, "Solo se puede terminar un trabajo que está IMPRIMIENDO");
        }
        // Con las hojas realmente usadas se calcula el costo final
        impresion.setHojasReales(hojasReales);
        impresion.setCosto(servicio.calcularCosto(impresion.getTipo(), hojasReales));
        impresion.setEstado("TERMINADO");
        return ResponseEntity.ok(servicio.guardar(impresion));
    }

    // PUT /api/impresiones/1/cancelar
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable("id") Long id) {
        Optional<Impresion> resultado = servicio.buscarPorId(id);
        if (resultado.isEmpty()) {
            return error(404, "Trabajo de impresión no encontrado");
        }
        Impresion impresion = resultado.get();
        if (impresion.getEstado().equals("TERMINADO") || impresion.getEstado().equals("CANCELADO")) {
            return error(409, "El trabajo ya terminó o ya estaba cancelado");
        }
        impresion.setEstado("CANCELADO");
        return ResponseEntity.ok(servicio.guardar(impresion));
    }

    // Respuesta de error con el mismo formato de la práctica: {"mensaje": "..."}
    private ResponseEntity<Map<String, String>> error(int codigo, String texto) {
        return ResponseEntity.status(codigo).body(Map.of("mensaje", texto));
    }
}