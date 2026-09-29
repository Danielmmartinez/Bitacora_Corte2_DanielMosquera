package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.ItemPedido;
import edu.dosw.restaurante.model.domain.Pedido;
import edu.dosw.restaurante.persistence.entity.ItemPedidoEntity;
import edu.dosw.restaurante.persistence.entity.PedidoEntity;
import org.mapstruct.Mapper;

/**
 * MapStruct usa itemToEntity / itemToDomain automáticamente para convertir la lista de ítems.
 */
@Mapper(componentModel = "spring")
public interface PedidoEntityMapper {

    PedidoEntity toEntity(Pedido pedido);

    Pedido toDomain(PedidoEntity entity);

    ItemPedidoEntity itemToEntity(ItemPedido item);

    ItemPedido itemToDomain(ItemPedidoEntity entity);
}
