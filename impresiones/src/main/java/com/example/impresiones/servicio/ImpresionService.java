package com.example.impresiones.servicio;

import com.example.impresiones.modelo.Impresion;
import com.example.impresiones.repositorio.ImpresionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ImpresionService {

    // Costo por hoja en pesos
    private static final double COSTO_BN = 0.50;
    private static final double COSTO_COLOR = 2.00;

    private final ImpresionRepository repositorio;

    public ImpresionService(ImpresionRepository repositorio) {
        this.repositorio = repositorio;
    }

    public double calcularCosto(String tipo, int hojas) {
        if (tipo.equals("COLOR")) {
            return COSTO_COLOR * hojas;
        }
        return COSTO_BN * hojas;
    }

    public Impresion crear(Impresion impresion) {
        impresion.setEstado("EN_COLA");
        impresion.setCosto(calcularCosto(impresion.getTipo(), impresion.getHojasEstimadas()));
        impresion.setFechaCreacion(LocalDateTime.now());
        return repositorio.save(impresion);
    }

    public Impresion guardar(Impresion impresion) {
        return repositorio.save(impresion);
    }

    public List<Impresion> obtenerTodas() {
        return repositorio.findAll();
    }

    public List<Impresion> obtenerCola() {
        return repositorio.findByEstadoOrderByIdAsc("EN_COLA");
    }

    public Optional<Impresion> buscarPorId(Long id) {
        return repositorio.findById(id);
    }
}