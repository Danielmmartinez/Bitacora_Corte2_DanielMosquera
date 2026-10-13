package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.mapper.UsuarioEntityMapper;
import edu.dosw.restaurante.mapper.UsuarioEntityMapperImpl;
import edu.dosw.restaurante.model.domain.Usuario;
import edu.dosw.restaurante.persistence.entity.UsuarioEntity;
import edu.dosw.restaurante.repository.UsuarioRepository;
import edu.dosw.restaurante.security.JwtUtil;
import edu.dosw.restaurante.security.Rol;
import edu.dosw.restaurante.security.Sesion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Spy
    private UsuarioEntityMapper usuarioMapper = new UsuarioEntityMapperImpl();

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private UsuarioEntity entidad(String email, Rol rol) {
        return UsuarioEntity.builder().id(1L).email(email).nombre("Nombre").password("$2a$hash").rol(rol).build();
    }

    @Test
    void login_Exito_DevuelveTokenYUsuario() {
        UserDetails principal = User.withUsername("mesero@sakura.com").password("$2a$hash").roles("MESERO").build();
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(jwtUtil.generarToken(principal)).thenReturn("token.jwt.firmado");
        when(jwtUtil.getExpiracionSegundos()).thenReturn(3600L);
        when(usuarioRepository.findByEmailIgnoreCase("mesero@sakura.com"))
                .thenReturn(Optional.of(entidad("mesero@sakura.com", Rol.MESERO)));

        Sesion sesion = authService.login("mesero@sakura.com", "Mesero123!");

        assertEquals("token.jwt.firmado", sesion.token());
        assertEquals(3600L, sesion.expiraEnSegundos());
        assertEquals(Rol.MESERO, sesion.usuario().getRol());
    }

    @Test
    void login_CredencialesIncorrectas_PropagaBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login("mesero@sakura.com", "mala"));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void registrarCliente_GuardaHashYRolCliente() {
        when(usuarioRepository.existsByEmailIgnoreCase("nuevo@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Secreta123")).thenReturn("$2a$10$hashGenerado");
        when(usuarioRepository.save(any())).thenAnswer(inv -> {
            UsuarioEntity e = inv.getArgument(0);
            e.setId(9L);
            return e;
        });

        Usuario creado = authService.registrarCliente("Nuevo", "nuevo@mail.com", "Secreta123");

        assertEquals(9L, creado.getId());
        assertEquals(Rol.CLIENTE, creado.getRol());
        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("$2a$10$hashGenerado", captor.getValue().getPassword()); // nunca el texto plano
    }

    @Test
    void registrarCliente_EmailExistente_LanzaConflicto() {
        when(usuarioRepository.existsByEmailIgnoreCase("gerente@sakura.com")).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> authService.registrarCliente("X", "gerente@sakura.com", "Secreta123"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void obtenerPorEmail_NoExiste_LanzaNoEncontrado() {
        when(usuarioRepository.findByEmailIgnoreCase("nadie@mail.com")).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> authService.obtenerPorEmail("nadie@mail.com"));
    }
}
