package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.persistence.entity.MesaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MesaEntityMapper {

    MesaEntity toEntity(Mesa mesa);

    Mesa toDomain(MesaEntity entity);
}
