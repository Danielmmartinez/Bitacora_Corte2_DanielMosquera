package edu.dosw.restaurante.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesaResponseDTO {
    private Long id;
    private Integer numero;
    private Integer capacidad;
    private String estado;
    private Boolean cuentaAbierta;
}