package com.example.ventas.integracion;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClienteClientes {
    private final RestClient restClient;

    public ClienteClientes(@Qualifier("clientesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void registrarConsumo(Integer clienteId, UUID operacionId, BigDecimal monto) {
        restClient.put()
                .uri("/api/clientes/{id}/consumos/{operacionId}", clienteId, operacionId)
                .body(Map.of("monto", monto))
                .retrieve().toBodilessEntity();
    }

    public void cancelarConsumo(Integer clienteId, UUID operacionId) {
        restClient.put()
                .uri("/api/clientes/{id}/consumos/{operacionId}/cancelacion", clienteId, operacionId)
                .retrieve().toBodilessEntity();
    }
}
