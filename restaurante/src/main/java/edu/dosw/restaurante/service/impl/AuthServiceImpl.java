package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.mapper.UsuarioEntityMapper;
import edu.dosw.restaurante.model.domain.Usuario;
import edu.dosw.restaurante.persistence.entity.UsuarioEntity;
import edu.dosw.restaurante.repository.UsuarioRepository;
import edu.dosw.restaurante.security.JwtUtil;
import edu.dosw.restaurante.security.Rol;
import edu.dosw.restaurante.security.Sesion;
import edu.dosw.restaurante.service.IAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEntityMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Sesion login(String email, String password) {
        // Compara la contraseña con el hash BCrypt. Si no coincide (o el email no existe)
        // lanza BadCredentialsException → 401. Nunca se dice cuál de los dos falló.
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));

        UserDetails usuarioAutenticado = (UserDetails) auth.getPrincipal();
        String token = jwtUtil.generarToken(usuarioAutenticado);
        log.info("Login exitoso: {}", email);
        return new Sesion(token, jwtUtil.getExpiracionSegundos(), obtenerPorEmail(email));
    }

    @Override
    @Transactional
    public Usuario registrarCliente(String nombre, String email, String password) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictoException("Ya existe un usuario con el email " + email);
        }
        // El registro público siempre crea CLIENTES: el personal lo crea el gerente/los datos iniciales
        UsuarioEntity guardado = usuarioRepository.save(UsuarioEntity.builder()
                .nombre(nombre)
                .email(email)
                .password(passwordEncoder.encode(password))
                .rol(Rol.CLIENTE)
                .build());
        log.info("Cliente registrado: {}", email);
        return usuarioMapper.toDomain(guardado);
    }

    @Override
    public Usuario obtenerPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .map(usuarioMapper::toDomain)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con email " + email));
    }
}
