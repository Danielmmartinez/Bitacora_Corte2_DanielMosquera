package edu.dosw.restaurante.security;

import edu.dosw.restaurante.model.domain.Usuario;

/**
 * Resultado de un login exitoso: el token y a quién pertenece.
 */
public record Sesion(String token, long expiraEnSegundos, Usuario usuario) {
}
