package edu.dosw.restaurante.model.domain;

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
    private Long idCuenta;
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

    public boolean puedeCambiarA(EstadoPedido nuevoEstado) {
        return estado != null && estado.puedeTransicionarA(nuevoEstado);
    }

    public void cambiarEstado(EstadoPedido nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public Double calcularTotal() {
        if (items == null) {
            return 0.0;
        }
        return items.stream().mapToDouble(ItemPedido::subtotal).sum();
    }
}
