package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.persistence.entity.PedidoEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Los ítems de un pedido se cargan de forma perezosa (LAZY). @EntityGraph le pide a
 * Hibernate traerlos en la misma consulta (JOIN) y así evitar una consulta extra por
 * cada pedido de la lista (el problema "N+1").
 */
@Repository
public interface PedidoRepository extends JpaRepository<PedidoEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "items")
    List<PedidoEntity> findAll();

    @EntityGraph(attributePaths = "items")
    List<PedidoEntity> findByEstado(EstadoPedido estado);

    @EntityGraph(attributePaths = "items")
    List<PedidoEntity> findByIdMesa(Long idMesa);

    @EntityGraph(attributePaths = "items")
    List<PedidoEntity> findByIdCuenta(Long idCuenta);

    // Para los reportes: pedidos creados en [desde, hasta)
    @EntityGraph(attributePaths = "items")
    @Query("SELECT p FROM PedidoEntity p WHERE p.timestamp >= :desde AND p.timestamp < :hasta")
    List<PedidoEntity> findCreadosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
