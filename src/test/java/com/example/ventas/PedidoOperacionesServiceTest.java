package com.example.ventas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.ventas.integracion.ClienteCatalogo;
import com.example.ventas.integracion.ClienteClientes;
import com.example.ventas.modelo.LineaReservaRemota;
import com.example.ventas.modelo.ReservaRemota;
import com.example.ventas.persistencia.PedidoEntity;
import com.example.ventas.persistencia.PedidoLineaEntity;
import com.example.ventas.persistencia.PedidoRepository;
import com.example.ventas.servicio.PedidoOperacionesService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PedidoOperacionesServiceTest {
    private PedidoRepository pedidos;
    private ClienteCatalogo catalogo;
    private ClienteClientes clientes;
    private PedidoOperacionesService service;

    @BeforeEach
    void preparar() {
        pedidos = mock(PedidoRepository.class);
        catalogo = mock(ClienteCatalogo.class);
        clientes = mock(ClienteClientes.class);
        service = new PedidoOperacionesService(pedidos, catalogo, clientes);
    }

    @Test
    void permiteCancelarUnPedidoPendienteYPropagaLaCancelacion() {
        UUID uuid = UUID.randomUUID();
        PedidoEntity pendiente = new PedidoEntity(uuid, 9, new BigDecimal("20.00"));
        pendiente.agregarLinea(new PedidoLineaEntity(2, "Lapiz", 2,
                new BigDecimal("10.00"), new BigDecimal("20.00")));
        when(pedidos.buscarConBloqueo(uuid)).thenReturn(Optional.of(pendiente));
        when(catalogo.cancelar(uuid))
                .thenReturn(new ReservaRemota(uuid, "CANCELADA", List.of(new LineaReservaRemota(2, 2))));

        var respuesta = service.cancelar(uuid);

        assertEquals("CANCELADO", respuesta.estado());
        verify(clientes).cancelarConsumo(9, uuid);
        verify(catalogo).cancelar(uuid);
    }

    @Test
    void confirmarReservaYConsumoCambiaElPedidoAConfirmado() {
        UUID uuid = UUID.randomUUID();
        PedidoEntity pendiente = new PedidoEntity(uuid, 9, new BigDecimal("20.00"));
        pendiente.agregarLinea(new PedidoLineaEntity(2, "Lapiz", 2,
                new BigDecimal("10.00"), new BigDecimal("20.00")));
        when(pedidos.buscarConBloqueo(uuid)).thenReturn(Optional.of(pendiente));
        when(catalogo.reservar(eq(uuid), any()))
                .thenReturn(new ReservaRemota(uuid, "CONFIRMADA", List.of(new LineaReservaRemota(2, 2))));

        var respuesta = service.confirmar(uuid);

        assertEquals("CONFIRMADO", respuesta.estado());
        verify(clientes).registrarConsumo(9, uuid, new BigDecimal("20.00"));
    }
}
