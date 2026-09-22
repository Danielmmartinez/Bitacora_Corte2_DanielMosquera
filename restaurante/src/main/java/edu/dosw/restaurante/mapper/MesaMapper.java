package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.model.dto.request.MesaRequestDTO;
import edu.dosw.restaurante.model.dto.response.MesaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MesaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", expression = "java(edu.dosw.restaurante.model.domain.EstadoMesa.DISPONIBLE)")
    @Mapping(target = "cuentaAbierta", constant = "false")
    Mesa toDomain(MesaRequestDTO dto);

    MesaResponseDTO toResponse(Mesa domain);

    List<MesaResponseDTO> toResponseList(List<Mesa> domainList);
}