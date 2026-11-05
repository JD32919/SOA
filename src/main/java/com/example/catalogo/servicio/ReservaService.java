/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.catalogo.servicio;

import com.example.catalogo.modelo.LineaReserva;
import com.example.catalogo.modelo.ReservaRespuesta;
import com.example.catalogo.modelo.SolicitudReserva;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservaService{
    private final JdbcTemplate jdbc;
    
    public ReservaService(DataSource dataSource){
        this.jdbc = new JdbcTemplate(dataSource);
    }
    
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ReservaRespuesta reservar(
            UUID operacionId,
            SolicitudReserva solicitud){
        
        List<LineaReserva> lineas=validar(operacionId, solicitud);
        String estado = abrirOperacion(operacionId);
        
        if("CANCELADA".equals(estado)){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La operacion fue cancelada y no puede reservar");
        }
        
        if("CONFIRMADA".equals(estado)){
            List<LineaReserva> guardadas = leerLineas(operacionId);
            if(!guardadas.equals(lineas)){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El UUID ya pertenece a otra solicitud");
            }
            return new ReservaRespuesta(operacionId, "CONFIRMADA", guardadas);
        }
        
        for(LineaReserva linea : lineas){
            descontar(linea);
            jdbc.update("""
                        INSERT INTO public.reserva_lineas(operacion_id, producto_id, cantidad)
                        Values (?,?,?)
                        """,
                    operacionId,
                    linea.productoId(),
                    linea.cantidad()
            );
        }
        jdbc.update("""
                    UPDATE public.reservas SET estado = 'CONFIRMADA' WHERE operacion_id = ? """, operacionId);
        
        return new ReservaRespuesta(operacionId, "CONFIRMADA", lineas);
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ReservaRespuesta cancelar(UUID operacionId){
        if(operacionId == null){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Envia un UUID valido"
            );
        }
        
        String estado=abrirOperacion(operacionId);
        List<LineaReserva> lineas= leerLineas(operacionId);
        
        if("CANCELADA".equals(estado)){
            return new ReservaRespuesta(operacionId, "CANCELADA", lineas);
        }
        if("CONFIRMADA".equals(estado)){
            for(LineaReserva linea : lineas){
                jdbc.update(
                        """
                        UPDATE public.productos
                        SET existencias = existencias + ?
                        WHERE id = ?
                        """,
                        linea.cantidad(),
                        linea.productoId()
                );
            }
        }
        
        jdbc.update("""
                    UPDATE public.reservas
                    SET estado = 'CANCELADA'
                    WHERE operacion_id = ?
                    """,
                operacionId
                );
        return new ReservaRespuesta(operacionId, "CANCELADA", lineas);
    }
    
    @Transactional(readOnly = true)
    public ReservaRespuesta consultar(UUID operacionId){
        List<String> estados = jdbc.query(
                """
                SELECT estado
                FROM public.reservas
                WHERE operacion_id = ?
                """,
                (fila, numero) -> fila.getString("estado"),
                operacionId
        );
        if (estados.isEmpty()){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Reserva no encontrada"
            );
        }
        return new ReservaRespuesta(
                operacionId,
                estados.get(0),
                leerLineas(operacionId)
        );
    }
    
    private String abrirOperacion(UUID operacionId){
        jdbc.update(
                """
                INSERT INTO public.reservas (operacion_id, estado)
                VALUES (?, 'PENDIENTE')
                ON CONFLICT (operacion_id) DO NOTHING
                """,
                operacionId
        );
        return jdbc.queryForObject(
                """
                SELECT estado
                FROM public.reservas
                WHERE operacion_id = ?
                FOR UPDATE
                """,
                String.class,
                operacionId
        );
    }
    
    private void descontar(LineaReserva linea){
        int filas = jdbc.update(
                """
                UPDATE public.productos
                SET existencias = existencias - ?
                WHERE id = ?
                AND activo = TRUE
                AND existencias >= ?
                """,
                linea.cantidad(),
                linea.productoId(),
                linea.cantidad()
        );
        if (filas == 1){
            return;
        }
        
        List<Boolean> activos =jdbc.query(
                "SELECT activo FROM public.productos WHERE id = ?",
                (fila, numero) -> fila.getBoolean("activo"),
                linea.productoId()
        );
        
        if(activos.isEmpty()){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Producto no encontrado: " + linea.productoId()
            );
        }
        
        if(!activos.get(0)){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El producto" + linea.productoId() + "no esta activo"
            );
        }
        
        throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Existencias insuficientes del producto" + linea.productoId()
        );
        
    }
    
    private List<LineaReserva> leerLineas(UUID operacionId){
        return jdbc.query(
                """
                SELECT producto_id, cantidad
                FROM public.reserva_lineas
                WHERE operacion_id = ?
                ORDER BY producto_id
                """,
                (fila, numero) -> new LineaReserva(
                        fila.getInt("producto_id"),
                        fila.getInt("cantidad")
                ),
                operacionId
        
        );
    }
    
    private List<LineaReserva> validar (UUID operacionId, SolicitudReserva solicitud){
        
        if(operacionId == null || solicitud == null || solicitud.lineas() == null || solicitud.lineas().isEmpty()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Envia en UUID y al menos una linea"
            );
        }
        
        Set<Integer> productos = new HashSet<>();
        
        for(LineaReserva linea : solicitud.lineas()){
            if(linea == null || linea.productoId() == null || linea.productoId() <= 0 || linea.cantidad() == null || linea.cantidad() <= 0){
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Cada linea necesita producto y cantidad positivos"
                );
            }
            if(!productos.add(linea.productoId())){
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Un producto no puede repetirse en la reserva"
                );
            }
        }
        return solicitud.lineas()
                .stream()
                .sorted(Comparator.comparing(LineaReserva::productoId))
                .toList();
    }   
}