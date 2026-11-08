/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.clientes.servicio;

import com.example.clientes.modelo.ClienteRespuesta;
import com.example.clientes.modelo.SolicitudCliente;
import com.example.clientes.modelo.SolicitudHabilitacion;
import com.example.clientes.modelo.SolicitudLimite;
import com.example.clientes.persistencia.ClienteEntity;
import com.example.clientes.persistencia.ClienteRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClienteService{
    private final ClienteRepository clienteRepository;
    
    public ClienteService(ClienteRepository clienteRepository){
    this.clienteRepository = clienteRepository;}
    
    @Transactional(readOnly = true)
    public List<ClienteRespuesta> obtenerTodos(){
        List<ClienteEntity> clientes_lista = clienteRepository.findAll(Sort.by("id"));
        List<ClienteRespuesta> res = new ArrayList<>();
        for(ClienteEntity c : clientes_lista){
            res.add(convertir(c));
        }
        return res;
    }
    
    // regresa null si no existe, el controller decide que hacer
    @Transactional(readOnly = true)
    public ClienteRespuesta buscarPorId(int id){
        ClienteEntity cliente = clienteRepository.findById(id).orElse(null);
        if(cliente == null){
            return null;
        }
        return convertir(cliente);
    }
    
    @Transactional
    public ClienteRespuesta crear(SolicitudCliente solicitud){
        validarDatos(solicitud);
        validarLimite(solicitud.limiteCredito());
        
        String email = solicitud.email().trim().toLowerCase();
        if(clienteRepository.existsByEmail(email)){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Ya existe un cliente con ese email"
            );
        }
        ClienteEntity cliente = clienteRepository.save(
                new ClienteEntity(solicitud.nombre().trim(), email, limpiarTelefono(solicitud.telefono()), solicitud.limiteCredito())
        );
        return convertir(cliente);
    }
    
    @Transactional
    public ClienteRespuesta actualizar(int id, SolicitudCliente solicitud){
        validarDatos(solicitud);
        ClienteEntity cliente = buscarEntidad(id);
        
        String email = solicitud.email().trim().toLowerCase();
        // si cambio el email hay que revisar que no lo tenga otro cliente
        if(!email.equals(cliente.getEmail()) && clienteRepository.existsByEmail(email)){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Ya existe un cliente con ese email"
            );
        }
        cliente.cambiarDatos(solicitud.nombre().trim(), email, limpiarTelefono(solicitud.telefono()));
        return convertir(cliente);
    }
    
    @Transactional
    public ClienteRespuesta cambiarHabilitacion(int id, SolicitudHabilitacion solicitud){
        if(solicitud == null || solicitud.habilitado() == null){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Indica si el cliente queda habilitado"
            );
        }
        ClienteEntity cliente = buscarEntidad(id);
        
        if(solicitud.habilitado()){
            cliente.cambiarHabilitacion(true, null);
        }else{
            String motivo = solicitud.motivo();
            if(motivo == null || motivo.isBlank() || motivo.trim().length() > 200){
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El motivo es obligatorio al inhabilitar (maximo 200 caracteres)"
                );
            }
            cliente.cambiarHabilitacion(false, motivo.trim());
        }
        return convertir(cliente);
    }
    
    @Transactional
    public ClienteRespuesta cambiarLimite(int id, SolicitudLimite solicitud){
        if(solicitud == null){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Envia el limite de credito"
            );
        }
        validarLimite(solicitud.limiteCredito());
        ClienteEntity cliente = buscarEntidad(id);
        // ojo: si el limite queda abajo de lo consumido solo bloquea compras nuevas
        cliente.cambiarLimite(solicitud.limiteCredito());
        return convertir(cliente);
    }


    private ClienteEntity buscarEntidad(int id){
        return clienteRepository
                .findById(id)
                .orElseThrow(()->new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Cliente no encontrado"
                ));
    }
    
    // aqui validamos nombre, email y telefono, se usa en crear y actualizar
    private void validarDatos(SolicitudCliente solicitud){
        if(solicitud==null || solicitud.nombre()==null || solicitud.nombre().isBlank() || solicitud.nombre().trim().length()>120){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El nombre es obligatorio"
            );
        }
        String mail = solicitud.email();
        if(mail==null || mail.trim().length()>160 || !mail.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El email no es valido"
            );
        }
        String tel = limpiarTelefono(solicitud.telefono());
        if(tel != null && !tel.matches("^[0-9+\\- ]{7,20}$")){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El telefono debe tener entre 7 y 20 digitos"
            );
        }
    }
    
    private void validarLimite(BigDecimal limite){
        if(limite==null || limite.compareTo(BigDecimal.ZERO)<0 || limite.stripTrailingZeros().scale()>2 || limite.compareTo(new BigDecimal("99999999.99"))>0){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El limite no puede ser negativo y maximo dos decimales"
            );
        }
    }
    
    private String limpiarTelefono(String telefono){
        if(telefono == null || telefono.isBlank()){
            return null;
        }
        return telefono.trim();
    }
    
    private ClienteRespuesta convertir(ClienteEntity entidad){
        return new ClienteRespuesta(
                entidad.getId(),
                entidad.getNombre(),
                entidad.getEmail(),
                entidad.getTelefono(),
                entidad.getLimiteCredito(),
                entidad.isHabilitado(),
                entidad.getMotivoInhabilitacion()
        );
    }
    
}

