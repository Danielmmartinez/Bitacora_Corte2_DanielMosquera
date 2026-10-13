package edu.dosw.restaurante.model.dto.response;

import java.time.LocalDate;
import java.util.Map;

public record ResumenDiaResponseDTO(
        LocalDate fecha,
        long totalPedidos,
        Map<String, Long> pedidosPorEstado,
        String platoMasVendido,
        long cuentasPagadas,
        double ingresosRestaurante,
        double ticketPromedio,
        long reservas,
        long vehiculosAtendidos,
        double ingresosParqueadero,
        long mesasOcupadasAhora,
        long mesasTotales,
        long vehiculosAdentroAhora) {
}
