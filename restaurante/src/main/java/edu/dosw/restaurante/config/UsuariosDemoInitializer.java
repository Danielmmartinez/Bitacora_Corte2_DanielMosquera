package edu.dosw.restaurante.config;

import edu.dosw.restaurante.persistence.entity.UsuarioEntity;
import edu.dosw.restaurante.repository.UsuarioRepository;
import edu.dosw.restaurante.security.Rol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea un usuario de prueba por rol al arrancar (solo si no existen).
 * Se activa con restaurante.seguridad.usuarios-demo=true. En producción debe ir en false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "restaurante.seguridad.usuarios-demo", havingValue = "true")
public class UsuariosDemoInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        crearSiNoExiste("gerente@sakura.com", "Gerente Sakura", "Gerente123!", Rol.GERENTE);
        crearSiNoExiste("mesero@sakura.com", "Mesero Sakura", "Mesero123!", Rol.MESERO);
        crearSiNoExiste("cocinero@sakura.com", "Cocinero Sakura", "Cocinero123!", Rol.COCINERO);
        crearSiNoExiste("cliente@sakura.com", "Cliente Sakura", "Cliente123!", Rol.CLIENTE);
    }

    private void crearSiNoExiste(String email, String nombre, String password, Rol rol) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        usuarioRepository.save(UsuarioEntity.builder()
                .email(email)
                .nombre(nombre)
                .password(passwordEncoder.encode(password)) // se guarda el hash, nunca el texto
                .rol(rol)
                .build());
        log.info("Usuario demo creado: {} ({})", email, rol);
    }
}
