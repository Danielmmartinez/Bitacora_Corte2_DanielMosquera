package edu.dosw.restaurante.model.dto.response;

import java.time.LocalDate;

public record IngresoDiarioResponseDTO(LocalDate fecha, long cuentasPagadas, double total) {
}
