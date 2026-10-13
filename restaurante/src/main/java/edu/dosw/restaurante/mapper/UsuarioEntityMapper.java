package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.Usuario;
import edu.dosw.restaurante.persistence.entity.UsuarioEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsuarioEntityMapper {

    // El dominio no tiene password, así que el hash no sale de la entidad
    Usuario toDomain(UsuarioEntity entity);
}
