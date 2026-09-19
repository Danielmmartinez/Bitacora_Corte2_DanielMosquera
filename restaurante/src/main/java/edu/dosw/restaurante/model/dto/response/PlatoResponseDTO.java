package edu.dosw.restaurante.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatoResponseDTO {
    private Long id;
    private String nombre;
    private Double precio;
    private String categoria;
    private Boolean disponible;
}