package edu.dosw.restaurante.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Algo que le pasó a un pedido (se creó, cambió de estado, se eliminó...).
 * Los eventos no se modifican una vez registrados: son un historial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoPedido {
    private String id;
    private Long idPedido;
    private Long idMesa;
    private TipoEventoPedido tipo;
    private EstadoPedido estadoAnterior;
    private EstadoPedido estadoNuevo;
    private LocalDateTime fecha;
    // Datos propios de cada tipo de evento (total, ítems...). Su forma varía de un evento a otro.
    private Map<String, Object> detalle;
}
