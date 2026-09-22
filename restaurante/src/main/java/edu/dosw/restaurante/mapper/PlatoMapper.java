package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.Plato;
import edu.dosw.restaurante.model.dto.request.PlatoRequestDTO;
import edu.dosw.restaurante.model.dto.response.PlatoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlatoMapper {

    @Mapping(target = "id", ignore = true)
    Plato toDomain(PlatoRequestDTO dto);

    PlatoResponseDTO toResponse(Plato domain);

    List<PlatoResponseDTO> toResponseList(List<Plato> domainList);
}