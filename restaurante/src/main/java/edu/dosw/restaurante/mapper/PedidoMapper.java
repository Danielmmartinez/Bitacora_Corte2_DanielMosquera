package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.domain.ItemPedido;
import edu.dosw.restaurante.domain.Pedido;
import edu.dosw.restaurante.dto.request.ItemPedidoRequestDTO;
import edu.dosw.restaurante.dto.request.PedidoRequestDTO;
import edu.dosw.restaurante.dto.response.ItemPedidoResponseDTO;
import edu.dosw.restaurante.dto.response.PedidoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PedidoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", expression = "java(edu.dosw.restaurante.domain.EstadoPedido.RECIBIDO)")
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