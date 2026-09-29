package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.EventoPedido;
import edu.dosw.restaurante.model.dto.response.EventoPedidoResponseDTO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Dominio → DTO de respuesta. Lo usa el Controller (capa de presentación).
 */
@Mapper(componentModel = "spring")
public interface EventoPedidoMapper {

    EventoPedidoResponseDTO toResponse(EventoPedido evento);

    List<EventoPedidoResponseDTO> toResponseList(List<EventoPedido> eventos);
}
