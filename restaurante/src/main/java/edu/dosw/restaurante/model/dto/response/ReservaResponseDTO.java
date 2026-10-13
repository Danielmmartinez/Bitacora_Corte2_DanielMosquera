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
public class ReservaResponseDTO {
    private Long id;
    private Long idMesa;
    private String nombreCliente;
    private String emailCliente;
    private LocalDateTime fechaHora;
    private Integer comensales;
    private String estado;
    private LocalDateTime fechaCreacion;
    private Long idCuenta;
}
