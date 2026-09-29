package edu.dosw.restaurante.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbrirCuentaRequestDTO {

    @NotNull(message = "El ID de la mesa es obligatorio")
    private Long idMesa;
}
