package com.example.impresiones.repositorio;

import com.example.impresiones.modelo.Impresion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImpresionRepository extends JpaRepository<Impresion, Long> {

    List<Impresion> findByEstadoOrderByIdAsc(String estado);
}