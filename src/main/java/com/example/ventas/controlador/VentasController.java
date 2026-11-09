package com.example.ventas.controlador;

import com.example.ventas.modelo.SolicitudVenta;
import com.example.ventas.modelo.VentaRespuesta;
import com.example.ventas.servicio.VentasService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
@RestController
@RequestMapping("/api/ventas")
public class VentasController {
    private final VentasService ventasService;

    public VentasController(VentasService ventasService) {
        this.ventasService = ventasService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaRespuesta crear(@RequestBody SolicitudVenta solicitud) {
        return ventasService.crear(solicitud);
    }

    @GetMapping
    public List<VentaRespuesta> listar() {
        return ventasService.listar();
    }

    @GetMapping("/{operacionId}")
    public VentaRespuesta consultar(@PathVariable UUID operacionId) {
        return ventasService.consultar(operacionId);
    }

    @PutMapping("/{operacionId}/cancelacion")
    public VentaRespuesta cancelar(@PathVariable UUID operacionId) {
        return ventasService.cancelar(operacionId);
    }
}
