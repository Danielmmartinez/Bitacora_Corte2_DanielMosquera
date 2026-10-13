package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.SolicitudInvalidaException;
import edu.dosw.restaurante.model.domain.*;
import edu.dosw.restaurante.model.domain.reporte.IngresoDiario;
import edu.dosw.restaurante.model.domain.reporte.PlatoVendido;
import edu.dosw.restaurante.model.domain.reporte.ReporteIngresos;
import edu.dosw.restaurante.model.domain.reporte.ResumenDia;
import edu.dosw.restaurante.service.*;
import edu.dosw.restaurante.util.CalculoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Reportes calculados con Streams sobre los datos de los demás servicios.
 * No tiene repositorio propio: solo lee a través de las interfaces de servicio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements IReporteService {

    private static final int MAX_DIAS_RANGO = 366;
    private static final String SIN_CATEGORIA = "Sin categoría";

    private final IPedidoService pedidoService;
    private final ICuentaService cuentaService;
    private final IPlatoService platoService;
    private final IMesaService mesaService;
    private final IReservaService reservaService;
    private final IParqueaderoService parqueaderoService;
    private final Clock clock;

    @Override
    public ResumenDia resumenDelDia(LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
        LocalDateTime desde = dia.atStartOfDay();
        LocalDateTime hasta = dia.plusDays(1).atStartOfDay();
        log.info("Generando resumen del {}", dia);

        List<Pedido> pedidos = pedidoService.obtenerCreadosEntre(desde, hasta);
        List<Cuenta> cuentas = cuentaService.obtenerCerradasEntre(desde, hasta);
        List<RegistroVehiculo> salidas = parqueaderoService.obtenerSalidasEntre(desde, hasta);
        List<Mesa> mesas = mesaService.obtenerTodas();

        // groupingBy = GROUP BY de SQL: cuenta cuántos pedidos hay en cada estado
        Map<EstadoPedido, Long> porEstado = pedidos.stream()
                .collect(Collectors.groupingBy(Pedido::getEstado, () -> new EnumMap<>(EstadoPedido.class), Collectors.counting()));

        double ingresos = sumar(cuentas.stream().mapToDouble(Cuenta::calcularTotal));
        double ingresosParqueadero = sumar(salidas.stream().mapToDouble(RegistroVehiculo::getCobro));
        String platoMasVendido = rankingDePlatos(pedidos).stream()
                .findFirst()
                .map(PlatoVendido::nombrePlato)
                .orElse(null);

        return new ResumenDia(
                dia,
                pedidos.size(),
                porEstado,
                platoMasVendido,
                cuentas.size(),
                ingresos,
                CalculoUtils.promedio(ingresos, cuentas.size()),
                reservaService.obtenerEntre(desde, hasta).stream().filter(r -> r.getEstado() != EstadoReserva.CANCELADA).count(),
                salidas.size(),
                ingresosParqueadero,
                mesas.stream().filter(Mesa::tieneCuentaAbierta).count(),
                mesas.size(),
                parqueaderoService.obtenerEstado().ocupados());
    }

    @Override
    public List<PlatoVendido> platosMasVendidos(LocalDate desde, LocalDate hasta, int limite) {
        if (limite < 1 || limite > 50) {
            throw new SolicitudInvalidaException("El límite debe estar entre 1 y 50");
        }
        Rango rango = validarRango(desde, hasta);
        List<Pedido> pedidos = pedidoService.obtenerCreadosEntre(rango.inicio(), rango.fin());
        return rankingDePlatos(pedidos).stream().limit(limite).toList();
    }

    @Override
    public ReporteIngresos ingresos(LocalDate desde, LocalDate hasta) {
        Rango rango = validarRango(desde, hasta);
        List<Cuenta> cuentas = cuentaService.obtenerCerradasEntre(rango.inicio(), rango.fin());
        List<RegistroVehiculo> salidas = parqueaderoService.obtenerSalidasEntre(rango.inicio(), rango.fin());

        // Por día: agrupa las cuentas por la fecha en que se pagaron (TreeMap = ordenado por fecha)
        List<IngresoDiario> porDia = cuentas.stream()
                .collect(Collectors.groupingBy(c -> c.getFechaCierre().toLocalDate(), TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new IngresoDiario(e.getKey(), e.getValue().size(),
                        sumar(e.getValue().stream().mapToDouble(Cuenta::calcularTotal))))
                .toList();

        Map<MetodoPago, Double> porMetodo = cuentas.stream()
                .filter(c -> c.getMetodoPago() != null)
                .collect(Collectors.groupingBy(Cuenta::getMetodoPago, () -> new EnumMap<>(MetodoPago.class),
                        Collectors.collectingAndThen(Collectors.summingDouble(Cuenta::calcularTotal), CalculoUtils::redondear)));

        double totalRestaurante = sumar(cuentas.stream().mapToDouble(Cuenta::calcularTotal));
        double totalParqueadero = sumar(salidas.stream().mapToDouble(RegistroVehiculo::getCobro));

        return new ReporteIngresos(rango.desde(), rango.hasta(), totalRestaurante, totalParqueadero,
                CalculoUtils.redondear(totalRestaurante + totalParqueadero), porDia, porMetodo,
                ingresosPorCategoria(rango));
    }

    // ── cálculos compartidos ────────────────────────────────────

    /** Unidades e ingresos por plato de los pedidos no cancelados, de mayor a menor. */
    private List<PlatoVendido> rankingDePlatos(List<Pedido> pedidos) {
        return pedidos.stream()
                .filter(p -> p.getEstado() != EstadoPedido.CANCELADO)
                .flatMap(p -> p.getItems().stream())        // lista de pedidos → lista de ítems
                .collect(Collectors.groupingBy(ItemPedido::getNombrePlato))
                .entrySet().stream()
                .map(e -> new PlatoVendido(
                        e.getKey(),
                        e.getValue().stream().mapToLong(ItemPedido::getCantidad).sum(),
                        sumar(e.getValue().stream().mapToDouble(ItemPedido::subtotal))))
                .sorted(Comparator.comparingLong(PlatoVendido::unidades).reversed()
                        .thenComparing(PlatoVendido::nombrePlato))
                .toList();
    }

    /**
     * Ingresos por categoría, calculados sobre los pedidos ENTREGADOS del rango.
     * La categoría se toma del plato actual; si el plato ya no existe se agrupa como "Sin categoría".
     */
    private Map<String, Double> ingresosPorCategoria(Rango rango) {
        Map<Long, String> categoriaPorPlato = platoService.obtenerTodos().stream()
                .collect(Collectors.toMap(Plato::getId, Plato::getCategoria));

        return pedidoService.obtenerCreadosEntre(rango.inicio(), rango.fin()).stream()
                .filter(p -> p.getEstado() == EstadoPedido.ENTREGADO)
                .flatMap(p -> p.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> categoriaPorPlato.getOrDefault(item.getIdPlato(), SIN_CATEGORIA),
                        TreeMap::new,
                        Collectors.collectingAndThen(Collectors.summingDouble(ItemPedido::subtotal), CalculoUtils::redondear)));
    }

    private static double sumar(DoubleStream valores) {
        return CalculoUtils.redondear(valores.sum());
    }

    private Rango validarRango(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate d = desde != null ? desde : hoy;
        LocalDate h = hasta != null ? hasta : d;
        if (d.isAfter(h)) {
            throw new SolicitudInvalidaException("La fecha 'desde' (" + d + ") es posterior a 'hasta' (" + h + ")");
        }
        if (ChronoUnit.DAYS.between(d, h) >= MAX_DIAS_RANGO) {
            throw new SolicitudInvalidaException("El rango no puede superar " + MAX_DIAS_RANGO + " días");
        }
        return new Rango(d, h);
    }

    /** [desde 00:00, (hasta + 1 día) 00:00): incluye completo el día "hasta". */
    private record Rango(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio() {
            return desde.atStartOfDay();
        }

        LocalDateTime fin() {
            return hasta.plusDays(1).atStartOfDay();
        }
    }
}
