package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.controller.docs.AuthApi;
import edu.dosw.restaurante.mapper.UsuarioMapper;
import edu.dosw.restaurante.model.dto.request.LoginRequestDTO;
import edu.dosw.restaurante.model.dto.request.RegistroRequestDTO;
import edu.dosw.restaurante.model.dto.response.TokenResponseDTO;
import edu.dosw.restaurante.model.dto.response.UsuarioResponseDTO;
import edu.dosw.restaurante.service.IAuthService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final IAuthService authService;
    private final UsuarioMapper usuarioMapper;

    @Override
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(usuarioMapper.toTokenResponse(
                authService.login(request.getEmail(), request.getPassword())));
    }

    @Override
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistroRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioMapper.toResponse(
                authService.registrarCliente(request.getNombre(), request.getEmail(), request.getPassword())));
    }

    // Spring inyecta el usuario que el JwtAuthFilter dejó en el SecurityContext
    @Override
    @GetMapping("/yo")
    public ResponseEntity<UsuarioResponseDTO> yo(@Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(usuarioMapper.toResponse(authService.obtenerPorEmail(authentication.getName())));
    }
}
