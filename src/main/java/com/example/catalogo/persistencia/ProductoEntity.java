/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.catalogo.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table (name= "productos", schema= "public")
public class ProductoEntity{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable=false, length=120)
    private String nombre;
    
    @Column(nullable=false, precision=10, scale=2)
    private BigDecimal precio;
    
    @Column(nullable=false)
    private int existencias;
    
    @Column(nullable=false)
    private boolean activo;
    
    protected ProductoEntity(){}
    
    public ProductoEntity(String nombre, BigDecimal precio, int existencias){
        this.nombre = nombre;
        this.precio = precio;
        this.existencias = existencias;
        this.activo = true;
    }
    
    public Integer getId(){
        return id;
    }
    
    public String getNombre(){
        return nombre;
    }
    
    public BigDecimal getPrecio(){
        return precio;
    }
    
    public int getExistencias(){
        return existencias;
    }
    
    public boolean isActivo(){
        return activo;
    }
    
    public void cambiarExistencias(int existencias){
        this.existencias=existencias;
    }
    
    
    
    
    
}

