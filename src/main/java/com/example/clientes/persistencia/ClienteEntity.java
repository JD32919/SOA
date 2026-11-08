/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.clientes.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table (name= "clientes", schema= "public")
public class ClienteEntity{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable=false, length=120)
    private String nombre;
    
    @Column(nullable=false, length=160, unique=true)
    private String email;
    
    @Column(length=20)
    private String telefono;
    
    @Column(name="limite_credito", nullable=false, precision=10, scale=2)
    private BigDecimal limiteCredito;
    
    @Column(nullable=false)
    private boolean habilitado;
    
    @Column(name="motivo_inhabilitacion", length=200)
    private String motivoInhabilitacion;
    
    @Column(name="creado_en", nullable=false, updatable=false)
    private OffsetDateTime creadoEn;
    
    protected ClienteEntity(){}
    
    public ClienteEntity(String nombre, String email, String telefono, BigDecimal limiteCredito){
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.limiteCredito = limiteCredito;
        this.habilitado = true;
        this.creadoEn = OffsetDateTime.now();
    }
    
    public Integer getId(){
        return id;
    }
    
    public String getNombre(){
        return nombre;
    }
    
    public String getEmail(){
        return email;
    }
    
    public String getTelefono(){
        return telefono;
    }
    
    public BigDecimal getLimiteCredito(){
        return limiteCredito;
    }
    
    public boolean isHabilitado(){
        return habilitado;
    }
    
    public String getMotivoInhabilitacion(){
        return motivoInhabilitacion;
    }
    
    public void cambiarDatos(String nombre, String email, String telefono){
        this.nombre=nombre;
        this.email=email;
        this.telefono=telefono;
    }
    
    public void cambiarLimite(BigDecimal limiteCredito){
        this.limiteCredito=limiteCredito;
    }
    
    public void cambiarHabilitacion(boolean habilitado, String motivo){
        this.habilitado=habilitado;
        this.motivoInhabilitacion=motivo;
    }
    
    
    
}

