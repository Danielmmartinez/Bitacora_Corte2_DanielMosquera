package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.RegistroVehiculo;
import edu.dosw.restaurante.persistence.entity.RegistroVehiculoEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RegistroVehiculoEntityMapper {

    RegistroVehiculoEntity toEntity(RegistroVehiculo registro);

    RegistroVehiculo toDomain(RegistroVehiculoEntity entity);
}
