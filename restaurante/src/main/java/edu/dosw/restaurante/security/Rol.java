package edu.dosw.restaurante.security;

/**
 * Roles del restaurante. Spring Security los maneja como "ROLE_GERENTE", "ROLE_MESERO"...
 * y en @PreAuthorize se escriben sin el prefijo: hasRole('GERENTE').
 */
public enum Rol {
    GERENTE,
    MESERO,
    COCINERO,
    CLIENTE
}
