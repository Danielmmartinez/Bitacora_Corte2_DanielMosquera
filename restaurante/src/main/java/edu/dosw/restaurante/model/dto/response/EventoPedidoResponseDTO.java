package edu.dosw.restaurante.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoPedidoResponseDTO {
    private String id;
    private Long idPedido;
    private Long idMesa;
    private String tipo;
    private String estadoAnterior;
    private String estadoNuevo;
    private LocalDateTime fecha;
    private Map<String, Object> detalle;
}
