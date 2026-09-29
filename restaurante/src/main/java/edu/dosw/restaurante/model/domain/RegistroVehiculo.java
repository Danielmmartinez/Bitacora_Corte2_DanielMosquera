package edu.dosw.restaurante.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Una estadía de un vehículo en el parqueadero: de la entrada a la salida.
 * Se cobra por hora o fracción, con un mínimo de una hora.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroVehiculo {
    private Long id;
    private String placa;
    private TipoVehiculo tipo;
    private LocalDateTime entrada;
    private LocalDateTime salida;
    private Double cobro;

    public boolean estaActivo() {
        return salida == null;
    }

    public long minutosEstacionado(LocalDateTime hasta) {
        return Duration.between(entrada, hasta).toMinutes();
    }

    /** 61 minutos = 2 horas. Nunca menos de 1 hora. */
    public long horasACobrar(LocalDateTime hasta) {
        long minutos = minutosEstacionado(hasta);
        long horas = (minutos + 59) / 60; // división entera redondeando hacia arriba
        return Math.max(1, horas);
    }

    public double calcularCobro(LocalDateTime hasta, double tarifaPorHora) {
        return horasACobrar(hasta) * tarifaPorHora;
    }

    public void registrarSalida(LocalDateTime momentoSalida, double tarifaPorHora) {
        this.salida = momentoSalida;
        this.cobro = calcularCobro(momentoSalida, tarifaPorHora);
    }
}
