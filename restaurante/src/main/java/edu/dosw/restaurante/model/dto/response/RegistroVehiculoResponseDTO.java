package edu.dosw.restaurante.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroVehiculoResponseDTO {
    private Long id;
    private String placa;
    private String tipo;
    private LocalDateTime entrada;
    private LocalDateTime salida;
    private Double cobro;
}
