package edu.dosw.restaurante.model.domain.reporte;

import java.time.LocalDate;

public record IngresoDiario(LocalDate fecha, long cuentasPagadas, double total) {
}
