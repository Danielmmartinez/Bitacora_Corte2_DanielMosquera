package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.Reserva;
import edu.dosw.restaurante.model.dto.request.ReservaRequestDTO;
import edu.dosw.restaurante.model.dto.response.ReservaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReservaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)        // lo asigna el Service (CONFIRMADA)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "idCuenta", ignore = true)
    Reserva toDomain(ReservaRequestDTO dto);

    ReservaResponseDTO toResponse(Reserva reserva);

    List<ReservaResponseDTO> toResponseList(List<Reserva> reservas);
}
