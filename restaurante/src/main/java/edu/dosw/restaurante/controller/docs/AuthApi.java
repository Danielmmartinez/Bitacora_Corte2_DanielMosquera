package edu.dosw.restaurante.controller.docs;

import edu.dosw.restaurante.model.dto.request.LoginRequestDTO;
import edu.dosw.restaurante.model.dto.request.RegistroRequestDTO;
import edu.dosw.restaurante.model.dto.response.ErrorResponseDTO;
import edu.dosw.restaurante.model.dto.response.TokenResponseDTO;
import edu.dosw.restaurante.model.dto.response.UsuarioResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "Autenticación", description = "Login con JWT y registro de clientes")
public interface AuthApi {

    @Operation(summary = "Iniciar sesión",
            description = "Devuelve un token JWT. Úsalo en el botón 'Authorize' de Swagger o en el header "
                    + "Authorization: Bearer <token>")
    @SecurityRequirements // público: no requiere token
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login correcto, token generado"),
            @ApiResponse(responseCode = "400", description = "Email o contraseña con formato inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<TokenResponseDTO> login(LoginRequestDTO request);

    @Operation(summary = "Registrarse como cliente", description = "Siempre crea un usuario con rol CLIENTE")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "El email ya está registrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<UsuarioResponseDTO> registrar(RegistroRequestDTO request);

    @Operation(summary = "Ver el usuario autenticado", description = "Útil para comprobar a quién pertenece un token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del usuario del token"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<UsuarioResponseDTO> yo(Authentication authentication);
}
