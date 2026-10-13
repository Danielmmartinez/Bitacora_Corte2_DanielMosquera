package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.SolicitudInvalidaException;
import edu.dosw.restaurante.model.domain.*;
import edu.dosw.restaurante.model.domain.reporte.PlatoVendido;
import edu.dosw.restaurante.model.domain.reporte.ReporteIngresos;
import edu.dosw.restaurante.model.domain.reporte.ResumenDia;
import edu.dosw.restaurante.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Datos de un día ficticio (15/06/2030) armados a mano, para comprobar cada número del reporte.
 */
@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

    private static final LocalDate HOY = LocalDate.of(2030, 6, 15);
    private static final LocalDate AYER = HOY.minusDays(1);

    @Mock private IPedidoService pedidoService;
    @Mock private ICuentaService cuentaService;
    @Mock private IPlatoService platoService;
    @Mock private IMesaService mesaService;
    @Mock private IReservaService reservaService;
    @Mock private IParqueaderoService parqueaderoService;

    private ReporteServiceImpl reporteService;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(HOY.atTime(20, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        reporteService = new ReporteServiceImpl(pedidoService, cuentaService, platoService, mesaService,
                reservaService, parqueaderoService, reloj);
    }

    private ItemPedido item(long idPlato, String nombre, double precio, int cantidad) {
        return ItemPedido.builder().idPlato(idPlato).nombrePlato(nombre).precioCongelado(precio).cantidad(cantidad).build();
    }

    private Pedido pedido(EstadoPedido estado, ItemPedido... items) {
        return Pedido.builder().estado(estado).items(List.of(items)).build();
    }

    // Ramen 2 u. + Gyoza 1 u. entregados; 5 Ramen cancelados; 3 Gyoza en cocina
    private List<Pedido> pedidosDeHoy() {
        return List.of(
                pedido(EstadoPedido.ENTREGADO, item(1, "Ramen", 25000, 2), item(2, "Gyoza", 8000, 1)),
                pedido(EstadoPedido.CANCELADO, item(1, "Ramen", 25000, 5)),
                pedido(EstadoPedido.EN_PREPARACION, item(2, "Gyoza", 8000, 3)));
    }

    private Cuenta cuenta(double total, MetodoPago metodo, LocalDateTime cierre) {
        return Cuenta.builder().estado(EstadoCuenta.CERRADA).total(total).metodoPago(metodo).fechaCierre(cierre).build();
    }

    private RegistroVehiculo salidaParqueadero(double cobro) {
        return RegistroVehiculo.builder().placa("ABC123").cobro(cobro).build();
    }

    @Test
    void resumenDelDia_CalculaTodosLosIndicadores() {
        when(pedidoService.obtenerCreadosEntre(HOY.atStartOfDay(), HOY.plusDays(1).atStartOfDay())).thenReturn(pedidosDeHoy());
        when(cuentaService.obtenerCerradasEntre(any(), any())).thenReturn(List.of(
                cuenta(58000, MetodoPago.EFECTIVO, HOY.atTime(14, 0)),
                cuenta(20000, MetodoPago.TARJETA, HOY.atTime(15, 0))));
        when(parqueaderoService.obtenerSalidasEntre(any(), any())).thenReturn(List.of(salidaParqueadero(5000)));
        when(mesaService.obtenerTodas()).thenReturn(List.of(
                Mesa.builder().cuentaAbierta(true).build(), Mesa.builder().cuentaAbierta(false).build(),
                Mesa.builder().cuentaAbierta(false).build()));
        when(reservaService.obtenerEntre(any(), any())).thenReturn(List.of(
                Reserva.builder().estado(EstadoReserva.CUMPLIDA).build(),
                Reserva.builder().estado(EstadoReserva.CANCELADA).build()));
        when(parqueaderoService.obtenerEstado()).thenReturn(new EstadoParqueadero(20, 4, 16, List.of()));

        ResumenDia r = reporteService.resumenDelDia(null); // null = hoy según el reloj

        assertEquals(HOY, r.fecha());
        assertEquals(3, r.totalPedidos());
        assertEquals(1L, r.pedidosPorEstado().get(EstadoPedido.CANCELADO));
        assertEquals("Gyoza", r.platoMasVendido()); // 4 unidades no canceladas vs 2 de Ramen
        assertEquals(2, r.cuentasPagadas());
        assertEquals(78000.0, r.ingresosRestaurante());
        assertEquals(39000.0, r.ticketPromedio());
        assertEquals(1, r.reservas());              // la cancelada no cuenta
        assertEquals(1, r.vehiculosAtendidos());
        assertEquals(5000.0, r.ingresosParqueadero());
        assertEquals(1, r.mesasOcupadasAhora());
        assertEquals(3, r.mesasTotales());
        assertEquals(4, r.vehiculosAdentroAhora());
    }

    @Test
    void resumenDelDia_SinMovimiento_TodoEnCero() {
        when(pedidoService.obtenerCreadosEntre(any(), any())).thenReturn(List.of());
        when(cuentaService.obtenerCerradasEntre(any(), any())).thenReturn(List.of());
        when(parqueaderoService.obtenerSalidasEntre(any(), any())).thenReturn(List.of());
        when(mesaService.obtenerTodas()).thenReturn(List.of());
        when(reservaService.obtenerEntre(any(), any())).thenReturn(List.of());
        when(parqueaderoService.obtenerEstado()).thenReturn(new EstadoParqueadero(20, 0, 20, List.of()));

        ResumenDia r = reporteService.resumenDelDia(HOY);

        assertEquals(0, r.totalPedidos());
        assertNull(r.platoMasVendido());
        assertEquals(0.0, r.ticketPromedio()); // sin dividir por cero
    }

    @Test
    void platosMasVendidos_RankingSinCanceladosYConLimite() {
        when(pedidoService.obtenerCreadosEntre(any(), any())).thenReturn(pedidosDeHoy());

        List<PlatoVendido> top = reporteService.platosMasVendidos(HOY, HOY, 5);

        assertEquals(2, top.size());
        assertEquals(new PlatoVendido("Gyoza", 4, 32000.0), top.get(0));
        assertEquals(new PlatoVendido("Ramen", 2, 50000.0), top.get(1));
        assertEquals(1, reporteService.platosMasVendidos(HOY, HOY, 1).size());
    }

    @Test
    void platosMasVendidos_LimiteInvalido() {
        assertThrows(SolicitudInvalidaException.class, () -> reporteService.platosMasVendidos(HOY, HOY, 0));
        assertThrows(SolicitudInvalidaException.class, () -> reporteService.platosMasVendidos(HOY, HOY, 51));
    }

    @Test
    void ingresos_DesglosePorDiaMetodoYCategoria() {
        when(cuentaService.obtenerCerradasEntre(AYER.atStartOfDay(), HOY.plusDays(1).atStartOfDay())).thenReturn(List.of(
                cuenta(58000, MetodoPago.EFECTIVO, HOY.atTime(14, 0)),
                cuenta(10000, MetodoPago.EFECTIVO, AYER.atTime(13, 0)),
                cuenta(20000, MetodoPago.TARJETA, HOY.atTime(15, 0))));
        when(parqueaderoService.obtenerSalidasEntre(any(), any())).thenReturn(List.of(salidaParqueadero(5000)));
        when(platoService.obtenerTodos()).thenReturn(List.of(
                Plato.builder().id(1L).categoria("Sopas").build()));  // el plato 2 fue eliminado
        when(pedidoService.obtenerCreadosEntre(any(), any())).thenReturn(pedidosDeHoy());

        ReporteIngresos r = reporteService.ingresos(AYER, HOY);

        assertEquals(88000.0, r.totalRestaurante());
        assertEquals(5000.0, r.totalParqueadero());
        assertEquals(93000.0, r.totalGeneral());
        assertEquals(2, r.porDia().size());
        assertEquals(AYER, r.porDia().get(0).fecha());          // ordenado por fecha
        assertEquals(78000.0, r.porDia().get(1).total());
        assertEquals(68000.0, r.porMetodoPago().get(MetodoPago.EFECTIVO));
        assertEquals(20000.0, r.porMetodoPago().get(MetodoPago.TARJETA));
        // Solo el pedido ENTREGADO: Ramen (Sopas) 50000 + Gyoza (plato eliminado) 8000
        assertEquals(50000.0, r.porCategoria().get("Sopas"));
        assertEquals(8000.0, r.porCategoria().get("Sin categoría"));
    }

    @Test
    void ingresos_RangoInvalido() {
        assertThrows(SolicitudInvalidaException.class, () -> reporteService.ingresos(HOY, AYER));
        assertThrows(SolicitudInvalidaException.class, () -> reporteService.ingresos(HOY.minusDays(400), HOY));
        verifyNoInteractions(cuentaService);
    }
}
