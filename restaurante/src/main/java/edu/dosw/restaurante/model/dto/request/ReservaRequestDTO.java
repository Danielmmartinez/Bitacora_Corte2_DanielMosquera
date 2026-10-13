package edu.dosw.restaurante.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaRequestDTO {

    @NotNull(message = "El ID de la mesa es obligatorio")
    private Long idMesa;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombreCliente;

    // Opcional para el personal (reservas por teléfono). Si reserva un CLIENTE, se usa el email de su token.
    @Email(message = "El email no tiene un formato válido")
    private String emailCliente;

    @NotNull(message = "La fecha y hora son obligatorias (formato 2026-10-01T19:30:00)")
    @Future(message = "La reserva debe ser para una fecha futura")
    private LocalDateTime fechaHora;

    @NotNull(message = "El número de comensales es obligatorio")
    @Min(value = 1, message = "Debe haber al menos 1 comensal")
    @Max(value = 20, message = "Para más de 20 personas comuníquese con el restaurante")
    private Integer comensales;
}
