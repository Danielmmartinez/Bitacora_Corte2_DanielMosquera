package edu.dosw.restaurante.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {
    private Long id;
    private Long idMesa;
    private List<ItemPedido> items;
    private EstadoPedido estado;
    private LocalDateTime timestamp;

    public Boolean puedeModificarse() {
        return EstadoPedido.RECIBIDO.equals(this.estado);
    }

    public void agregarItem(ItemPedido item) {
        if (Boolean.TRUE.equals(puedeModificarse())) {
            if (this.items == null) {
                this.items = new ArrayList<>();
            }
            this.items.add(item);
        }
    }

    public void cambiarEstado(EstadoPedido nuevoEstado) {
        this.estado = nuevoEstado;
    }
}