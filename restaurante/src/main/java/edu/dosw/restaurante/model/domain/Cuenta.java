package edu.dosw.restaurante.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * La cuenta de una mesa: agrupa los pedidos desde que se sientan hasta que pagan.
 * Ciclo: ABIERTA → (el cliente la pide) EN_PAGO → (paga) CERRADA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cuenta {
    private Long id;
    private Long idMesa;
    private EstadoCuenta estado;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private Double total;           // se congela al pagar
    private MetodoPago metodoPago;
    private Double montoRecibido;

    // No se persiste con la cuenta: el Service la llena con los pedidos asociados
    @Builder.Default
    private List<Pedido> pedidos = new ArrayList<>();

    public boolean estaCerrada() {
        return EstadoCuenta.CERRADA.equals(estado);
    }

    /** Total de los pedidos no cancelados. Una vez pagada, es el total congelado. */
    public Double calcularTotal() {
        if (estaCerrada() && total != null) {
            return total;
        }
        if (pedidos == null) {
            return 0.0;
        }
        return pedidos.stream()
                .filter(p -> p.getEstado() != EstadoPedido.CANCELADO)
                .mapToDouble(Pedido::calcularTotal)
                .sum();
    }

    /** Pedidos que la cocina todavía no ha entregado: impiden pagar. */
    public List<Pedido> pedidosPendientes() {
        if (pedidos == null) {
            return List.of();
        }
        return pedidos.stream()
                .filter(p -> p.getEstado() != null && !p.getEstado().esFinal())
                .toList();
    }

    public void solicitarPago() {
        this.estado = EstadoCuenta.EN_PAGO;
    }

    public void pagar(MetodoPago metodo, Double monto, LocalDateTime momento) {
        this.total = calcularTotal();
        this.metodoPago = metodo;
        this.montoRecibido = monto;
        this.fechaCierre = momento;
        this.estado = EstadoCuenta.CERRADA;
    }

    public Double calcularCambio() {
        if (montoRecibido == null || total == null) {
            return null;
        }
        return montoRecibido - total;
    }
}
