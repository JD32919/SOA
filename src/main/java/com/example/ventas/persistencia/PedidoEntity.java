package com.example.ventas.persistencia;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedidos")
public class PedidoEntity {
    @Id
    @Column(name = "operacion_id", nullable = false)
    private UUID operacionId;

    @Column(name = "cliente_id", nullable = false)
    private Integer clienteId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PedidoLineaEntity> lineas = new ArrayList<>();

    protected PedidoEntity() {}

    public PedidoEntity(UUID operacionId, Integer clienteId, BigDecimal total) {
        this.operacionId = operacionId;
        this.clienteId = clienteId;
        this.total = total;
        this.estado = "PENDIENTE";
        this.creadoEn = OffsetDateTime.now();
        this.actualizadoEn = this.creadoEn;
    }

    public void agregarLinea(PedidoLineaEntity linea) {
        lineas.add(linea);
        linea.asignarPedido(this);
    }

    public void cambiarEstado(String estado) {
        this.estado = estado;
        this.actualizadoEn = OffsetDateTime.now();
    }

    public UUID getOperacionId() { return operacionId; }
    public Integer getClienteId() { return clienteId; }
    public BigDecimal getTotal() { return total; }
    public String getEstado() { return estado; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
    public List<PedidoLineaEntity> getLineas() { return lineas; }
}
