package edu.dosw.restaurante.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRequestDTO {

    @NotNull(message = "El ID de la mesa es obligatorio")
    private Long idMesa;

    @NotEmpty(message = "El pedido debe contener al menos un ítem")
    @Valid
    private List<ItemPedidoRequestDTO> items;
}