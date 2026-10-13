package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.EstadoParqueadero;
import edu.dosw.restaurante.model.domain.RegistroVehiculo;
import edu.dosw.restaurante.model.dto.response.EstadoParqueaderoResponseDTO;
import edu.dosw.restaurante.model.dto.response.RegistroVehiculoResponseDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ParqueaderoMapper {

    RegistroVehiculoResponseDTO toResponse(RegistroVehiculo registro);

    List<RegistroVehiculoResponseDTO> toResponseList(List<RegistroVehiculo> registros);

    EstadoParqueaderoResponseDTO toResponse(EstadoParqueadero estado);
}
