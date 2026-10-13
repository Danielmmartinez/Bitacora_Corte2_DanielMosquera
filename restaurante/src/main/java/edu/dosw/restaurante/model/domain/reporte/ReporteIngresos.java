package edu.dosw.restaurante.model.domain.reporte;

import edu.dosw.restaurante.model.domain.MetodoPago;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ReporteIngresos(
        LocalDate desde,
        LocalDate hasta,
        double totalRestaurante,
        double totalParqueadero,
        double totalGeneral,
        List<IngresoDiario> porDia,
        Map<MetodoPago, Double> porMetodoPago,
        Map<String, Double> porCategoria) {
}
