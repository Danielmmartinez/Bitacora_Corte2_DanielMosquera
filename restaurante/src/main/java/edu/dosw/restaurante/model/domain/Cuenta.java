package edu.dosw.restaurante.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cuenta {
    private Long id;
    private Long idMesa;
    private Double total;
    private EstadoCuenta estado;
    private LocalDateTime fechaApertura;

    public Double calcularTotal() {
        return total != null ? total : 0.0;
    }

    public void registrarPago() {
        this.estado = EstadoCuenta.EN_PAGO;
    }

    public void cerrarCuenta() {
        this.estado = EstadoCuenta.CERRADA;
    }
}