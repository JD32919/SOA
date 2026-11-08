/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.clientes.servicio;

import com.example.clientes.modelo.ConsumoRespuesta;
import com.example.clientes.modelo.CreditoRespuesta;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Ventas registra aqui lo que compra cada cliente usando el mismo UUID que manda a catalogo,
// si algo falla en el proceso cancela con ese UUID. Las dos cosas se pueden repetir sin problema
@Service
public class ConsumoService{
    private final JdbcTemplate jdbc;
    
    public ConsumoService(DataSource dataSource){
        this.jdbc = new JdbcTemplate(dataSource);
    }
    
    @Transactional(readOnly = true)
    public CreditoRespuesta consultarCredito(int clienteId, BigDecimal monto){
        if(monto != null){
            validarMonto(monto);
        }
        Map<String, Object> cliente = leerCliente(clienteId, false);
        return evaluar(clienteId, cliente, monto);
    }
    
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ConsumoRespuesta registrar(int clienteId, UUID operacionId, BigDecimal monto){
        if(operacionId == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envia un UUID valido");
        }
        validarMonto(monto);
        
        // FOR UPDATE para que dos pedidos al mismo tiempo no se pasen del limite
        Map<String, Object> cliente = leerCliente(clienteId, true);
        
        ConsumoRespuesta previo = leerConsumo(operacionId);
        if(previo != null){
            if("CANCELADO".equals(previo.estado())){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La operacion fue cancelada y no puede registrarse");
            }
            if(previo.clienteId() != clienteId || previo.monto().compareTo(monto) != 0){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El UUID ya pertenece a otra solicitud");
            }
            // ya estaba registrado, se regresa lo mismo
            return previo;
        }
        
        CreditoRespuesta credito = evaluar(clienteId, cliente, monto);
        if(!credito.puedeComprar()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, credito.motivo());
        }
        
        jdbc.update("""
                    INSERT INTO public.consumos(operacion_id, cliente_id, monto, estado)
                    VALUES (?,?,?,'CONFIRMADO')
                    """,
                operacionId,
                clienteId,
                monto
        );
        return leerConsumo(operacionId);
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ConsumoRespuesta cancelar(int clienteId, UUID operacionId){
        if(operacionId == null){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Envia un UUID valido"
            );
        }
        leerCliente(clienteId, true);
        ConsumoRespuesta previo = leerConsumo(operacionId);
        
        if(previo == null){
            // la cancelacion llego antes que el registro, se guarda como cancelado
            // para que si despues llega el registro tardio se rechace
            jdbc.update("""
                        INSERT INTO public.consumos(operacion_id, cliente_id, monto, estado)
                        VALUES (?,?,0,'CANCELADO')
                        """,
                    operacionId,
                    clienteId
            );
            return leerConsumo(operacionId);
        }
        if(previo.clienteId() != clienteId){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El UUID pertenece a otro cliente"
            );
        }
        if("CONFIRMADO".equals(previo.estado())){
            jdbc.update("""
                        UPDATE public.consumos
                        SET estado = 'CANCELADO', actualizado_en = now()
                        WHERE operacion_id = ?
                        """,
                    operacionId
            );
        }
        return leerConsumo(operacionId);
    }
    
    @Transactional(readOnly = true)
    public List<ConsumoRespuesta> historial(int clienteId){
        leerCliente(clienteId, false);
        // TODO: paginar esto despues, por ahora regresa todo
        return jdbc.query(
                """
                SELECT operacion_id, cliente_id, monto, estado, registrado_en
                FROM public.consumos
                WHERE cliente_id = ?
                ORDER BY registrado_en DESC, operacion_id
                """,
                (fila, numero) -> new ConsumoRespuesta(
                        fila.getObject("operacion_id", UUID.class),
                        fila.getInt("cliente_id"),
                        fila.getBigDecimal("monto"),
                        fila.getString("estado"),
                        fila.getObject("registrado_en", OffsetDateTime.class)
                ),
                clienteId
        );
    }
    
    private CreditoRespuesta evaluar(int clienteId, Map<String, Object> cliente, BigDecimal monto){
        BigDecimal consumido = jdbc.queryForObject(
                """
                SELECT COALESCE(SUM(monto), 0)
                FROM public.consumos
                WHERE cliente_id = ? AND estado = 'CONFIRMADO'
                """,
                BigDecimal.class,
                clienteId
        );
        boolean habilitado = (Boolean) cliente.get("habilitado");
        BigDecimal limite = (BigDecimal) cliente.get("limite_credito");
        BigDecimal disponible = limite.subtract(consumido);
        if(disponible.compareTo(BigDecimal.ZERO) < 0){
            disponible = BigDecimal.ZERO;
        }
        //System.out.println("credito cliente " + clienteId + " consumido=" + consumido + " disponible=" + disponible);
        
        boolean puede = true;
        String motivo = null;
        if(!habilitado){
            puede = false;
            motivo = "El cliente esta inhabilitado";
            if(cliente.get("motivo_inhabilitacion") != null){
                motivo = motivo + ": " + cliente.get("motivo_inhabilitacion");
            }
        }else if(monto != null && monto.compareTo(disponible) > 0){
            puede = false;
            motivo = "El monto supera el credito disponible (" + disponible.toPlainString() + ")";
        }
        return new CreditoRespuesta(clienteId, habilitado, limite, consumido, disponible, puede, motivo);
    }
    
    private Map<String, Object> leerCliente(int clienteId, boolean bloquear){
        String sql = "SELECT habilitado, limite_credito, motivo_inhabilitacion FROM public.clientes WHERE id = ?";
        if(bloquear){
            sql = sql + " FOR UPDATE";
        }
        List<Map<String, Object>> filas = jdbc.queryForList(sql, clienteId);
        if(filas.isEmpty()){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Cliente no encontrado"
            );
        }
        return filas.get(0);
    }
    
    private ConsumoRespuesta leerConsumo(UUID operacionId){
        List<ConsumoRespuesta> temp = jdbc.query(
                """
                SELECT operacion_id, cliente_id, monto, estado, registrado_en
                FROM public.consumos
                WHERE operacion_id = ?
                """,
                (fila, numero) -> new ConsumoRespuesta(
                        fila.getObject("operacion_id", UUID.class),
                        fila.getInt("cliente_id"),
                        fila.getBigDecimal("monto"),
                        fila.getString("estado"),
                        fila.getObject("registrado_en", OffsetDateTime.class)
                ),
                operacionId
        );
        if(temp.isEmpty()){
            return null;
        }
        return temp.get(0);
    }
    
    private void validarMonto(BigDecimal monto){
        if(monto==null || monto.compareTo(BigDecimal.ZERO)<=0 || monto.stripTrailingZeros().scale()>2 || monto.compareTo(new BigDecimal("99999999.99"))>0){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El monto debe ser mayor a cero y maximo dos decimales"
            );
        }
    }
}
