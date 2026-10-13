package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.persistence.document.EventoPedidoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Igual que un JpaRepository, pero para MongoDB: Spring Data genera la implementación
 * y traduce los nombres de los métodos a consultas de Mongo (no a SQL).
 */
@Repository
public interface EventoPedidoRepository extends MongoRepository<EventoPedidoDocument, String> {

    // db.eventos_pedido.find({ idPedido: ? }).sort({ fecha: 1 })
    List<EventoPedidoDocument> findByIdPedidoOrderByFechaAsc(Long idPedido);
}
