package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.Cuenta;
import edu.dosw.restaurante.persistence.entity.CuentaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CuentaEntityMapper {

    CuentaEntity toEntity(Cuenta cuenta);

    @Mapping(target = "pedidos", ignore = true) // los carga el Service desde PedidoRepository
    Cuenta toDomain(CuentaEntity entity);
}
