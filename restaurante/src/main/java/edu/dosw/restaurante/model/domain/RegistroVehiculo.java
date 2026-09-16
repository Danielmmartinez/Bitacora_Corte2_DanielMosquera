package edu.dosw.restaurante.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Duration;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroVehiculo {
    private Long id;
    private String placa;
    private LocalDateTime entrada;
    private LocalDateTime salida;
    private Double cobro;

    public Double calcularCobro() {
        if (entrada == null || salida == null) {
            return 0.0;
        }
        long horas = Duration.between(entrada, salida).toHours();
        if (horas == 0) horas = 1;
        return horas * 5000.0; // Tarifa base simulada por hora
    }

    public void registrarSalida() {
        this.salida = LocalDateTime.now();
        this.cobro = calcularCobro();
    }

    public Boolean estaActivo() {
        return salida == null;
    }
}