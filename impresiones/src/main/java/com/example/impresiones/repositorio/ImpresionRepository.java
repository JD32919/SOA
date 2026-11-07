package com.example.impresiones.repositorio;

import com.example.impresiones.modelo.Impresion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImpresionRepository extends JpaRepository<Impresion, Long> {

    // Trabajos de un estado, del más antiguo al más nuevo (sirve para la cola)
    List<Impresion> findByEstadoOrderByIdAsc(String estado);
}