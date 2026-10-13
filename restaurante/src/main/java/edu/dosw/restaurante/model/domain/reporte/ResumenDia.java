package edu.dosw.restaurante.model.domain.reporte;

import edu.dosw.restaurante.model.domain.EstadoPedido;

import java.time.LocalDate;
import java.util.Map;

/**
 * Resumen operativo de un día. Los datos "de ahora" (mesas ocupadas, vehículos dentro)
 * solo tienen sentido cuando la fecha es hoy.
 */
public record ResumenDia(
        LocalDate fecha,
        long totalPedidos,
        Map<EstadoPedido, Long> pedidosPorEstado,
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
