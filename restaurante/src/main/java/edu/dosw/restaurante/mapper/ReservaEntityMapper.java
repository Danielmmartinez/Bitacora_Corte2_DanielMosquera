package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.Reserva;
import edu.dosw.restaurante.persistence.entity.ReservaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservaEntityMapper {

    ReservaEntity toEntity(Reserva reserva);

    Reserva toDomain(ReservaEntity entity);
}
