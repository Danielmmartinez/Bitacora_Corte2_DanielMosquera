package edu.dosw.restaurante.model.domain;

import edu.dosw.restaurante.security.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Usuario del sistema. A propósito no tiene contraseña: el hash solo vive en la entidad
 * y nunca sale de la capa de persistencia/seguridad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    private Long id;
    private String email;
    private String nombre;
    private Rol rol;
}
