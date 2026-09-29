package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.Usuario;
import edu.dosw.restaurante.model.dto.response.TokenResponseDTO;
import edu.dosw.restaurante.model.dto.response.UsuarioResponseDTO;
import edu.dosw.restaurante.security.Sesion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    UsuarioResponseDTO toResponse(Usuario usuario);

    @Mapping(target = "tipo", constant = "Bearer")
    TokenResponseDTO toTokenResponse(Sesion sesion);
}
