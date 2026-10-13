package edu.dosw.restaurante.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponseDTO {
    private String token;
    private String tipo;               // siempre "Bearer"
    private Long expiraEnSegundos;
    private UsuarioResponseDTO usuario;
}
