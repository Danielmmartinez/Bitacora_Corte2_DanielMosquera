package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.ItemPedido;
import edu.dosw.restaurante.model.domain.Pedido;
import edu.dosw.restaurante.model.dto.request.ItemPedidoRequestDTO;
import edu.dosw.restaurante.model.dto.request.PedidoRequestDTO;
import edu.dosw.restaurante.model.dto.response.ItemPedidoResponseDTO;
import edu.dosw.restaurante.model.dto.response.PedidoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PedidoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", expression = "java(edu.dosw.restaurante.model.domain.EstadoPedido.RECIBIDO)")
    @Mapping(target = "timestamp", expression = "java(java.time.LocalDateTime.now())")
    Pedido toDomain(PedidoRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nombrePlato", ignore = true)
    @Mapping(target = "precioCongelado", ignore = true)
    ItemPedido itemToDomain(ItemPedidoRequestDTO dto);

    PedidoResponseDTO toResponse(Pedido domain);

    @Mapping(target = "subtotal", expression = "java(domain.subtotal())")
    ItemPedidoResponseDTO itemToResponse(ItemPedido domain);

    List<PedidoResponseDTO> toResponseList(List<Pedido> domainList);
}