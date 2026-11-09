package com.example.ventas.servicio;

import com.example.ventas.integracion.ClienteCatalogo;
import com.example.ventas.integracion.ClienteSoapClientes;
import com.example.ventas.modelo.LineaVentaSolicitud;
import com.example.ventas.modelo.ProductoRemoto;
import com.example.ventas.modelo.SolicitudVenta;
import com.example.ventas.modelo.VentaRespuesta;
import com.example.ventas.persistencia.PedidoEntity;
import com.example.ventas.persistencia.PedidoLineaEntity;
import com.example.ventas.persistencia.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VentasService {
    private final PedidoRepository pedidos;
    private final ClienteCatalogo catalogo;
    private final ClienteSoapClientes clientesSoap;
    private final PedidoOperacionesService operaciones;

    public VentasService(PedidoRepository pedidos, ClienteCatalogo catalogo,
                         ClienteSoapClientes clientesSoap, PedidoOperacionesService operaciones) {
        this.pedidos = pedidos;
        this.catalogo = catalogo;
        this.clientesSoap = clientesSoap;
        this.operaciones = operaciones;
    }

    public VentaRespuesta crear(SolicitudVenta solicitud) {
        List<LineaVentaSolicitud> lineas = validar(solicitud);
        UUID uuid = solicitud.operacionId();

        VentaRespuesta existente = operaciones.buscarExistente(uuid, solicitud.clienteId(), lineas);
        if (existente != null) {
            if ("CONFIRMADO".equals(existente.estado())) return existente;
            return operaciones.confirmar(uuid);
        }

        List<PedidoLineaEntity> detalle = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (LineaVentaSolicitud linea : lineas) {
            ProductoRemoto producto = llamar(() -> catalogo.consultarProducto(linea.productoId()));
            if (producto == null || producto.id() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Catalogo devolvio una respuesta vacia al consultar un producto");
            }
            if (!Boolean.TRUE.equals(producto.activo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El producto " + linea.productoId() + " no esta activo");
            }
            if (producto.precio() == null || producto.precio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Catalogo devolvio un precio invalido para el producto " + linea.productoId());
            }

            BigDecimal precio = producto.precio().setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(linea.cantidad()))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(subtotal);
            detalle.add(new PedidoLineaEntity(producto.id(), producto.nombre(),
                    linea.cantidad(), precio, subtotal));
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        ClienteSoapClientes.CreditoSoap credito = clientesSoap.consultarCredito(solicitud.clienteId());
        if (!credito.habilitado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El cliente esta inhabilitado");
        }
        if (credito.disponible().compareTo(total) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pedido supera el credito disponible (" + credito.disponible().toPlainString() + ")");
        }

        operaciones.preparar(uuid, solicitud.clienteId(), total, detalle, lineas);
        return operaciones.confirmar(uuid);
    }

    @Transactional(readOnly = true)
    public List<VentaRespuesta> listar() {
        return pedidos.listarConLineas().stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public VentaRespuesta consultar(UUID operacionId) {
        PedidoEntity pedido = pedidos.findById(operacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        return convertir(pedido);
    }

    public VentaRespuesta cancelar(UUID operacionId) {
        return operaciones.cancelar(operacionId);
    }

    private List<LineaVentaSolicitud> validar(SolicitudVenta s) {
        if (s == null || s.operacionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere un UUID de operacion valido");
        }
        if (s.clienteId() == null || s.clienteId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El clienteId debe ser un entero positivo");
        }
        if (s.lineas() == null || s.lineas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido debe contener al menos un producto");
        }
        Set<Integer> ids = new HashSet<>();
        for (LineaVentaSolicitud linea : s.lineas()) {
            if (linea == null || linea.productoId() == null || linea.productoId() <= 0
                    || linea.cantidad() == null || linea.cantidad() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada linea necesita productoId y cantidad positivos");
            }
            if (!ids.add(linea.productoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No repitas productos; agrupa su cantidad en una sola linea");
            }
        }
        return s.lineas().stream().sorted(Comparator.comparing(LineaVentaSolicitud::productoId)).toList();
    }

    private VentaRespuesta convertir(PedidoEntity p) {
        var lineas = p.getLineas().stream()
                .sorted(Comparator.comparing(PedidoLineaEntity::getProductoId))
                .map(l -> new com.example.ventas.modelo.LineaVentaRespuesta(l.getProductoId(),
                        l.getNombreProducto(), l.getCantidad(), l.getPrecioUnitario(), l.getSubtotal()))
                .toList();
        return new VentaRespuesta(p.getOperacionId(), p.getClienteId(), p.getTotal(),
                p.getEstado(), p.getCreadoEn(), lineas);
    }

    private <T> T llamar(java.util.function.Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Un servicio proveedor no respondio dentro del tiempo limite", e);
        } catch (RestClientResponseException e) {
            int codigo = e.getStatusCode().value();
            String detalle = e.getResponseBodyAsString();
            HttpStatus destino = (codigo == 400 || codigo == 404 || codigo == 409)
                    ? HttpStatus.valueOf(codigo) : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(destino,
                    "Catalogo: HTTP " + codigo + (detalle.isBlank() ? "" : " - " + detalle), e);
        }
    }
}
