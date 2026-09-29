package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.Plato;
import edu.dosw.restaurante.persistence.entity.PlatoEntity;
import org.mapstruct.Mapper;

/**
 * Traduce entre el dominio y la entidad JPA. Lo usa el ServiceImpl (capa de persistencia);
 * el Controller sigue usando PlatoMapper (capa de presentación).
 */
@Mapper(componentModel = "spring")
public interface PlatoEntityMapper {

    PlatoEntity toEntity(Plato plato);

    Plato toDomain(PlatoEntity entity);
}
