package com.example.ventas.servicio;

import com.example.ventas.integracion.ClienteCatalogo;
import com.example.ventas.integracion.ClienteClientes;
import com.example.ventas.modelo.LineaReservaRemota;
import com.example.ventas.modelo.LineaVentaRespuesta;
import com.example.ventas.modelo.LineaVentaSolicitud;
import com.example.ventas.modelo.ReservaRemota;
import com.example.ventas.modelo.SolicitudReservaRemota;
import com.example.ventas.modelo.VentaRespuesta;
import com.example.ventas.persistencia.PedidoEntity;
import com.example.ventas.persistencia.PedidoLineaEntity;
import com.example.ventas.persistencia.PedidoRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PedidoOperacionesService {
    private final PedidoRepository pedidos;
    private final ClienteCatalogo catalogo;
    private final ClienteClientes clientes;

    public PedidoOperacionesService(PedidoRepository pedidos, ClienteCatalogo catalogo,
                                    ClienteClientes clientes) {
        this.pedidos = pedidos;
        this.catalogo = catalogo;
        this.clientes = clientes;
    }

    @Transactional(readOnly = true)
    public VentaRespuesta buscarExistente(UUID uuid, Integer clienteId,
                                           List<LineaVentaSolicitud> lineas) {
        PedidoEntity existente = pedidos.findById(uuid).orElse(null);
        if (existente == null) return null;
        comprobarMismaSolicitud(existente, clienteId, lineas);
        if ("CANCELADO".equals(existente.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La operacion fue cancelada y no puede reutilizarse");
        }
        return convertir(existente);
    }

    @Transactional
    public PedidoEntity preparar(UUID uuid, Integer clienteId, BigDecimal total,
                                 List<PedidoLineaEntity> detalle,
                                 List<LineaVentaSolicitud> solicitudLineas) {
        PedidoEntity existente = pedidos.buscarConBloqueo(uuid).orElse(null);
        if (existente != null) {
            comprobarMismaSolicitud(existente, clienteId, solicitudLineas);
            if ("CANCELADO".equals(existente.getEstado())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La operacion fue cancelada y no puede reutilizarse");
            }
            return existente;
        }

        PedidoEntity pedido = new PedidoEntity(uuid, clienteId, total);
        detalle.forEach(pedido::agregarLinea);
        return pedidos.saveAndFlush(pedido);
    }

    @Transactional
    public VentaRespuesta confirmar(UUID uuid) {
        PedidoEntity pedido = pedidos.buscarConBloqueo(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido pendiente no encontrado"));
        if ("CONFIRMADO".equals(pedido.getEstado())) return convertir(pedido);
        if ("CANCELADO".equals(pedido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La operacion fue cancelada y no puede confirmarse");
        }

        List<LineaReservaRemota> lineas = pedido.getLineas().stream()
                .map(l -> new LineaReservaRemota(l.getProductoId(), l.getCantidad()))
                .toList();

        boolean reservaIntentada = false;
        boolean consumoIntentado = false;
        try {
            reservaIntentada = true;
            ReservaRemota reserva = llamar(() -> catalogo.reservar(uuid,
                    new SolicitudReservaRemota(lineas)));
            if (reserva == null || !"CONFIRMADA".equals(reserva.estado())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Catalogo no confirmo la reserva del inventario");
            }

            consumoIntentado = true;
            llamar(() -> {
                clientes.registrarConsumo(pedido.getClienteId(), uuid, pedido.getTotal());
                return Boolean.TRUE;
            });

            pedido.cambiarEstado("CONFIRMADO");
            pedidos.flush();
            return convertir(pedido);
        } catch (RuntimeException error) {
            boolean compensado = true;
            if (consumoIntentado) compensado &= intentar(() -> clientes.cancelarConsumo(pedido.getClienteId(), uuid));
            if (reservaIntentada) compensado &= intentar(() -> catalogo.cancelar(uuid));
            if (compensado) {
                pedido.cambiarEstado("CANCELADO");
                pedidos.flush();
            }
            throw error;
        }
    }

    @Transactional
    public VentaRespuesta cancelar(UUID uuid) {
        PedidoEntity pedido = pedidos.buscarConBloqueo(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if ("CANCELADO".equals(pedido.getEstado())) return convertir(pedido);

        // Tambien se puede cancelar PENDIENTE. Ambos proveedores guardan el UUID
        // cancelado para rechazar una confirmacion tardia.
        llamar(() -> {
            clientes.cancelarConsumo(pedido.getClienteId(), uuid);
            return Boolean.TRUE;
        });
        llamar(() -> catalogo.cancelar(uuid));

        pedido.cambiarEstado("CANCELADO");
        pedidos.flush();
        return convertir(pedido);
    }

    private void comprobarMismaSolicitud(PedidoEntity pedido, Integer clienteId,
                                         List<LineaVentaSolicitud> lineas) {
        if (!pedido.getClienteId().equals(clienteId) || pedido.getLineas().size() != lineas.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El UUID ya pertenece a otra solicitud");
        }
        List<PedidoLineaEntity> guardadas = pedido.getLineas().stream()
                .sorted(Comparator.comparing(PedidoLineaEntity::getProductoId)).toList();
        for (int i = 0; i < lineas.size(); i++) {
            if (!guardadas.get(i).getProductoId().equals(lineas.get(i).productoId())
                    || !guardadas.get(i).getCantidad().equals(lineas.get(i).cantidad())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El UUID ya pertenece a otra solicitud");
            }
        }
    }

    private VentaRespuesta convertir(PedidoEntity p) {
        List<LineaVentaRespuesta> lineas = p.getLineas().stream()
                .sorted(Comparator.comparing(PedidoLineaEntity::getProductoId))
                .map(l -> new LineaVentaRespuesta(l.getProductoId(), l.getNombreProducto(),
                        l.getCantidad(), l.getPrecioUnitario(), l.getSubtotal()))
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
                    "Servicio proveedor: HTTP " + codigo + (detalle.isBlank() ? "" : " - " + detalle), e);
        }
    }

    private boolean intentar(Runnable accion) {
        try {
            accion.run();
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
