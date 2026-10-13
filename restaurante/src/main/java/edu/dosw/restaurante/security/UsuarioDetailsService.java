package edu.dosw.restaurante.security;

import edu.dosw.restaurante.persistence.entity.UsuarioEntity;
import edu.dosw.restaurante.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Le dice a Spring Security cómo cargar un usuario por su "username" (aquí, el email).
 * Lo usan el login (para comparar la contraseña) y el filtro JWT (para saber los roles).
 */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con email " + email));

        return User.withUsername(usuario.getEmail())
                .password(usuario.getPassword())   // hash BCrypt
                .roles(usuario.getRol().name())    // se convierte en la authority "ROLE_" + rol
                .build();
    }
}
