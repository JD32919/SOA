package com.example.ventas.persistencia;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<PedidoEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PedidoEntity p where p.operacionId = :id")
    Optional<PedidoEntity> buscarConBloqueo(@Param("id") UUID id);

    @Query("select distinct p from PedidoEntity p left join fetch p.lineas order by p.creadoEn desc")
    List<PedidoEntity> listarConLineas();
}
