package com.example.ventas.integracion;

import com.example.ventas.modelo.ProductoRemoto;
import com.example.ventas.modelo.ReservaRemota;
import com.example.ventas.modelo.SolicitudReservaRemota;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClienteCatalogo {
    private final RestClient restClient;

    public ClienteCatalogo(@Qualifier("catalogoRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ProductoRemoto consultarProducto(Integer id) {
        return restClient.get().uri("/api/productos/{id}", id)
                .retrieve().body(ProductoRemoto.class);
    }

    public ReservaRemota reservar(UUID operacionId, SolicitudReservaRemota solicitud) {
        return restClient.put().uri("/api/reservas/{id}", operacionId)
                .body(solicitud).retrieve().body(ReservaRemota.class);
    }

    public ReservaRemota cancelar(UUID operacionId) {
        return restClient.put().uri("/api/reservas/{id}/cancelacion", operacionId)
                .retrieve().body(ReservaRemota.class);
    }
}
