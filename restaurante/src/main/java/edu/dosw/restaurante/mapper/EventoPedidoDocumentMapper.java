package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.EventoPedido;
import edu.dosw.restaurante.persistence.document.EventoPedidoDocument;
import org.mapstruct.Mapper;

/**
 * Dominio ↔ documento Mongo. Lo usa el Service del historial (capa de persistencia).
 */
@Mapper(componentModel = "spring")
public interface EventoPedidoDocumentMapper {

    EventoPedidoDocument toDocument(EventoPedido evento);

    EventoPedido toDomain(EventoPedidoDocument document);
}
