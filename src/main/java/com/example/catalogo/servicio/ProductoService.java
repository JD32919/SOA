/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.catalogo.servicio;

import com.example.catalogo.modelo.ProductoRespuesta;
import com.example.catalogo.modelo.SolicitudExistencias;
import com.example.catalogo.modelo.SolicitudProducto;
import com.example.catalogo.persistencia.ProductoEntity;
import com.example.catalogo.persistencia.ProductoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductoService{
    private final ProductoRepository productoRepository;
    
    public ProductoService(ProductoRepository productoRepository){
    this.productoRepository = productoRepository;}
    
    @Transactional(readOnly = true)
    public List<ProductoRespuesta> obtenerTodos(){
        return productoRepository
                .findAll(Sort.by("id"))
                .stream()
                .map(this::convertir)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public Optional<ProductoRespuesta> buscarPorId(int id){
        return productoRepository
                .findById(id)
                .map(this::convertir);
    }
    
    @Transactional
    public ProductoRespuesta crear(SolicitudProducto solicitud){
        if(solicitud==null || solicitud.nombre()==null || solicitud.nombre().isBlank() || solicitud.nombre().trim().length()>120){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El nombre es obligatorio"
            );
        }
        if(solicitud.precio()==null || solicitud.precio().compareTo(BigDecimal.ZERO)<=0 || solicitud.precio().stripTrailingZeros().scale()>2){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El precio debe ser mayor a cero y maximo dos decimales"
            );
        }
        if(solicitud.existencias()==null || solicitud.existencias()<0){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Las existencias no pueden ser negativas"
            );
        }
        
        String nombre =solicitud.nombre().trim();
        if(productoRepository.existsByNombre(nombre)){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Ya existe un producto con ese nombre"
            );
        }
        ProductoEntity producto = productoRepository.save(
                new ProductoEntity(nombre, solicitud.precio(),solicitud.existencias())
        );
        return convertir(producto);
    }
    
    @Transactional
    public ProductoRespuesta cambiarExistencias(int id, SolicitudExistencias solicitud){
        if(solicitud == null || solicitud.existencias()==null || solicitud.existencias()<0){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Las existencias no pueden ser negativas"
            );
        
        }
        ProductoEntity producto = productoRepository
                .findById(id)
                .orElseThrow(()->new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Producto no encontrado"
                ));
        producto.cambiarExistencias(solicitud.existencias());
        return convertir(producto);
    }
    private ProductoRespuesta convertir(ProductoEntity entidad){
        return new ProductoRespuesta(
                entidad.getId(),
                entidad.getNombre(),
                entidad.getPrecio(),
                entidad.getExistencias(),
                entidad.isActivo()               
        );
    }
    
}

