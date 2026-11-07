package com.example.impresiones.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "impresiones")
public class Impresion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clienteId;
    private String documento;
    private String tipo;            // "BN" o "COLOR"
    private int hojasEstimadas;
    private Integer hojasReales;    // se llena al terminar
    private double costo;
    private String estado;          // EN_COLA, IMPRIMIENDO, TERMINADO, CANCELADO
    private LocalDateTime fechaCreacion;

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getHojasEstimadas() {
        return hojasEstimadas;
    }

    public void setHojasEstimadas(int hojasEstimadas) {
        this.hojasEstimadas = hojasEstimadas;
    }

    public Integer getHojasReales() {
        return hojasReales;
    }

    public void setHojasReales(Integer hojasReales) {
        this.hojasReales = hojasReales;
    }

    public double getCosto() {
        return costo;
    }

    public void setCosto(double costo) {
        this.costo = costo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}