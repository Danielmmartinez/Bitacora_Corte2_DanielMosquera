package edu.dosw.restaurante.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntradaVehiculoRequestDTO {

    // Acepta mayúsculas o minúsculas y un espacio o guion opcional: "ABC123", "abc-123", "ABC 12D"
    @NotBlank(message = "La placa es obligatoria")
    @Pattern(regexp = "(?i)^[A-Z]{3}[ -]?[0-9]{2}[0-9A-Z]$",
            message = "Placa inválida. Formatos: ABC123 (carro) o ABC12D (moto)")
    private String placa;
}
