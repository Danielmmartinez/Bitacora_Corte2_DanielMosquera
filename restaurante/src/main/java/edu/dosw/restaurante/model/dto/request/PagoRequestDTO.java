package edu.dosw.restaurante.model.dto.request;

import edu.dosw.restaurante.model.domain.MetodoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoRequestDTO {

    @NotNull(message = "El método de pago es obligatorio (EFECTIVO, TARJETA o TRANSFERENCIA)")
    private MetodoPago metodoPago;

    @NotNull(message = "El monto recibido es obligatorio")
    @PositiveOrZero(message = "El monto recibido no puede ser negativo")
    private Double montoRecibido;
}
