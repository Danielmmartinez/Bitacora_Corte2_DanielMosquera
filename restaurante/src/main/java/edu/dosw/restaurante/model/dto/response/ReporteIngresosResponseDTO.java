package edu.dosw.restaurante.model.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ReporteIngresosResponseDTO(
        LocalDate desde,
        LocalDate hasta,
        double totalRestaurante,
        double totalParqueadero,
        double totalGeneral,
        List<IngresoDiarioResponseDTO> porDia,
        Map<String, Double> porMetodoPago,
        Map<String, Double> porCategoria) {
}
