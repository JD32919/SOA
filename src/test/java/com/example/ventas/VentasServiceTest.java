package com.example.ventas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.ventas.integracion.ClienteCatalogo;
import com.example.ventas.integracion.ClienteSoapClientes;
import com.example.ventas.modelo.LineaVentaRespuesta;
import com.example.ventas.modelo.LineaVentaSolicitud;
import com.example.ventas.modelo.ProductoRemoto;
import com.example.ventas.modelo.SolicitudVenta;
import com.example.ventas.modelo.VentaRespuesta;
import com.example.ventas.persistencia.PedidoRepository;
import com.example.ventas.servicio.PedidoOperacionesService;
import com.example.ventas.servicio.VentasService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class VentasServiceTest {
    private PedidoRepository pedidos;
    private ClienteCatalogo catalogo;
    private ClienteSoapClientes clientesSoap;
    private PedidoOperacionesService operaciones;
    private VentasService service;

    @BeforeEach
    void preparar() {
        pedidos = mock(PedidoRepository.class);
        catalogo = mock(ClienteCatalogo.class);
        clientesSoap = mock(ClienteSoapClientes.class);
        operaciones = mock(PedidoOperacionesService.class);
        service = new VentasService(pedidos, catalogo, clientesSoap, operaciones);
        when(operaciones.buscarExistente(any(), anyInt(), anyList())).thenReturn(null);
        when(clientesSoap.consultarCredito(anyInt()))
                .thenReturn(new ClienteSoapClientes.CreditoSoap(true, new BigDecimal("1000.00")));
    }

    @Test
    void preparaYConfirmaPedidoCalculandoElTotal() {
        UUID uuid = UUID.randomUUID();
        when(catalogo.consultarProducto(1))
                .thenReturn(new ProductoRemoto(1, "Cuaderno", new BigDecimal("38.00"), 50, true));
        when(operaciones.confirmar(uuid)).thenReturn(new VentaRespuesta(uuid, 7,
                new BigDecimal("76.00"), "CONFIRMADO", OffsetDateTime.now(),
                List.of(new LineaVentaRespuesta(1, "Cuaderno", 2,
                        new BigDecimal("38.00"), new BigDecimal("76.00")))));

        var respuesta = service.crear(new SolicitudVenta(uuid, 7, List.of(new LineaVentaSolicitud(1, 2))));

        assertEquals("CONFIRMADO", respuesta.estado());
        assertEquals(new BigDecimal("76.00"), respuesta.total());
        verify(operaciones).preparar(eq(uuid), eq(7), eq(new BigDecimal("76.00")), anyList(), anyList());
        verify(operaciones).confirmar(uuid);
    }

    @Test
    void rechazaPedidoSinLineas() {
        assertThrows(ResponseStatusException.class,
                () -> service.crear(new SolicitudVenta(UUID.randomUUID(), 1, List.of())));
        verifyNoInteractions(catalogo, clientesSoap, operaciones);
    }

    @Test
    void rechazaProductoInactivo() {
        when(catalogo.consultarProducto(1))
                .thenReturn(new ProductoRemoto(1, "Producto", new BigDecimal("10.00"), 4, false));

        assertThrows(ResponseStatusException.class,
                () -> service.crear(new SolicitudVenta(UUID.randomUUID(), 1,
                        List.of(new LineaVentaSolicitud(1, 1)))));
        verify(operaciones, never()).preparar(any(), any(), any(), anyList(), anyList());
    }

    @Test
    void rechazaPedidoCuandoElCreditoEsInsuficiente() {
        when(clientesSoap.consultarCredito(1))
                .thenReturn(new ClienteSoapClientes.CreditoSoap(true, new BigDecimal("5.00")));
        when(catalogo.consultarProducto(1))
                .thenReturn(new ProductoRemoto(1, "Cuaderno", new BigDecimal("38.00"), 50, true));

        assertThrows(ResponseStatusException.class,
                () -> service.crear(new SolicitudVenta(UUID.randomUUID(), 1,
                        List.of(new LineaVentaSolicitud(1, 1)))));
        verify(operaciones, never()).preparar(any(), any(), any(), anyList(), anyList());
    }

    @Test
    void reintentoDePedidoConfirmadoNoVuelveALlamarProveedores() {
        UUID uuid = UUID.randomUUID();
        VentaRespuesta anterior = new VentaRespuesta(uuid, 7, new BigDecimal("76.00"),
                "CONFIRMADO", OffsetDateTime.now(), List.of());
        when(operaciones.buscarExistente(eq(uuid), eq(7), anyList())).thenReturn(anterior);

        VentaRespuesta respuesta = service.crear(new SolicitudVenta(uuid, 7,
                List.of(new LineaVentaSolicitud(1, 2))));

        assertEquals(anterior, respuesta);
        verifyNoInteractions(catalogo, clientesSoap);
        verify(operaciones, never()).confirmar(any());
    }
}
