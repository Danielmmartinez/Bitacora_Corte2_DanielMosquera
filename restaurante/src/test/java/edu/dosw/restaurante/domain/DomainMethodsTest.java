package edu.dosw.restaurante.domain;

import edu.dosw.restaurante.model.domain.*;
import edu.dosw.restaurante.util.CalculoUtils;
import edu.dosw.restaurante.util.PlacaUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainMethodsTest {

    @Test
    void testPlatoDisponibilidad() {
        Plato plato = Plato.builder().nombre("Nigiri").precio(15000.0).disponible(true).build();
        assertTrue(plato.estaDisponible());

        plato.desactivar();
        assertFalse(plato.estaDisponible());

        plato.activar();
        assertTrue(plato.estaDisponible());

        Plato sinDato = Plato.builder().disponible(null).build();
        assertFalse(sinDato.estaDisponible());
    }

    @Test
    void testEstadoPedidoTransiciones() {
        assertTrue(EstadoPedido.RECIBIDO.puedeTransicionarA(EstadoPedido.EN_PREPARACION));
        assertTrue(EstadoPedido.RECIBIDO.puedeTransicionarA(EstadoPedido.CANCELADO));
        assertFalse(EstadoPedido.RECIBIDO.puedeTransicionarA(EstadoPedido.ENTREGADO));
        assertTrue(EstadoPedido.EN_PREPARACION.puedeTransicionarA(EstadoPedido.LISTO));
        assertFalse(EstadoPedido.EN_PREPARACION.puedeTransicionarA(EstadoPedido.CANCELADO));
        assertTrue(EstadoPedido.LISTO.puedeTransicionarA(EstadoPedido.ENTREGADO));
        assertFalse(EstadoPedido.ENTREGADO.puedeTransicionarA(EstadoPedido.RECIBIDO));
        assertFalse(EstadoPedido.CANCELADO.puedeTransicionarA(EstadoPedido.RECIBIDO));

        assertTrue(EstadoPedido.RECIBIDO.esCancelable());
        assertFalse(EstadoPedido.LISTO.esCancelable());
    }

    @Test
    void testPedidoTotalYTransicion() {
        Pedido pedido = Pedido.builder().estado(EstadoPedido.RECIBIDO).items(List.of(
                ItemPedido.builder().precioCongelado(10000.0).cantidad(2).build(),
                ItemPedido.builder().precioCongelado(5000.0).cantidad(1).build())).build();

        assertEquals(25000.0, pedido.calcularTotal());
        assertTrue(pedido.puedeCambiarA(EstadoPedido.EN_PREPARACION));
        assertFalse(pedido.puedeCambiarA(EstadoPedido.LISTO));

        assertEquals(0.0, Pedido.builder().build().calcularTotal());
        assertFalse(Pedido.builder().build().puedeCambiarA(EstadoPedido.LISTO));
    }

    @Test
    void testPedidoNoAgregaItemsFueraDeRecibido() {
        Pedido pedido = Pedido.builder().estado(EstadoPedido.LISTO).build();
        pedido.agregarItem(ItemPedido.builder().idPlato(1L).cantidad(1).build());
        assertNull(pedido.getItems());
    }

    @Test
    void testPedidoMetodos() {
        Pedido pedido = Pedido.builder().estado(EstadoPedido.RECIBIDO).build();
        assertTrue(pedido.puedeModificarse());

        ItemPedido item = ItemPedido.builder().idPlato(1L).cantidad(2).build();
        pedido.agregarItem(item);
        assertEquals(1, pedido.getItems().size());

        pedido.cambiarEstado(EstadoPedido.EN_PREPARACION);
        assertFalse(pedido.puedeModificarse());
    }

    @Test
    void testMesaMetodos() {
        Mesa mesa = Mesa.builder().estado(EstadoMesa.DISPONIBLE).cuentaAbierta(false).build();
        assertTrue(mesa.estaDisponible());
        assertFalse(mesa.tieneCuentaAbierta());

        mesa.abrirCuenta(7L);
        assertTrue(mesa.tieneCuentaAbierta());
        assertEquals(7L, mesa.getIdCuentaAbierta());
        assertEquals(EstadoMesa.OCUPADA, mesa.getEstado());
        assertTrue(mesa.getCuentaAbierta());

        mesa.cerrarCuenta();
        assertEquals(EstadoMesa.DISPONIBLE, mesa.getEstado());
        assertFalse(mesa.getCuentaAbierta());
        assertNull(mesa.getIdCuentaAbierta());
    }

    private Reserva reserva(long idMesa, LocalDateTime inicio) {
        return Reserva.builder().idMesa(idMesa).fechaHora(inicio).estado(EstadoReserva.CONFIRMADA)
                .emailCliente("ana@mail.com").build();
    }

    @Test
    void testReservaSolapamiento() {
        LocalDateTime siete = LocalDateTime.of(2030, 1, 1, 19, 0);
        Reserva base = reserva(1, siete);

        assertTrue(base.seSolapaCon(reserva(1, siete.plusMinutes(90)), 120));   // 19:00-21:00 vs 20:30
        assertTrue(base.seSolapaCon(reserva(1, siete.minusMinutes(119)), 120)); // 17:01-19:01: se cruza 1 minuto
        assertFalse(base.seSolapaCon(reserva(1, siete.plusMinutes(120)), 120)); // empieza justo al terminar
        assertFalse(base.seSolapaCon(reserva(2, siete), 120));                  // otra mesa

        Reserva cancelada = reserva(1, siete);
        cancelada.cancelar();
        assertFalse(base.seSolapaCon(cancelada, 120));                          // las canceladas no bloquean

        // Una reserva todavía sin estado (por crear) sí se compara contra las confirmadas
        Reserva porCrear = Reserva.builder().idMesa(1L).fechaHora(siete.plusMinutes(30)).build();
        assertTrue(porCrear.seSolapaCon(base, 120));
    }

    @Test
    void testReservaCicloDeVida() {
        LocalDateTime ahora = LocalDateTime.of(2030, 1, 1, 10, 0);
        Reserva r = reserva(1, ahora.plusHours(9));

        assertTrue(r.estaVigente(ahora));
        assertFalse(r.estaVigente(ahora.plusDays(1)));
        assertTrue(r.perteneceA("ANA@mail.com"));
        assertFalse(r.perteneceA("otro@mail.com"));
        assertFalse(r.perteneceA(null));
        assertEquals(ahora.plusHours(11), r.fin(120));

        r.reprogramar(ahora.plusHours(10));
        assertEquals(ahora.plusHours(10), r.getFechaHora());

        r.registrarLlegada();
        assertEquals(EstadoReserva.CUMPLIDA, r.getEstado());
        assertFalse(r.estaVigente(ahora));

        r.cancelar();
        assertEquals(EstadoReserva.CANCELADA, r.getEstado());
    }

    @Test
    void testTipoVehiculoPorPlaca() {
        assertEquals(TipoVehiculo.CARRO, TipoVehiculo.desdePlaca("ABC123").orElseThrow());
        assertEquals(TipoVehiculo.MOTO, TipoVehiculo.desdePlaca("ABC12D").orElseThrow());
        assertTrue(TipoVehiculo.desdePlaca("AB1234").isEmpty());
        assertTrue(TipoVehiculo.desdePlaca(null).isEmpty());
    }

    @Test
    void testRegistroVehiculoCobroPorHoraOFraccion() {
        LocalDateTime entrada = LocalDateTime.of(2030, 1, 1, 12, 0);
        RegistroVehiculo r = RegistroVehiculo.builder().placa("ABC123").tipo(TipoVehiculo.CARRO).entrada(entrada).build();

        assertTrue(r.estaActivo());
        assertEquals(1, r.horasACobrar(entrada.plusMinutes(5)));    // mínimo 1 hora
        assertEquals(1, r.horasACobrar(entrada.plusMinutes(60)));
        assertEquals(2, r.horasACobrar(entrada.plusMinutes(61)));   // la fracción cuenta como hora
        assertEquals(2, r.horasACobrar(entrada.plusMinutes(119)));  // antes: toHours() truncaba a 1
        assertEquals(10000.0, r.calcularCobro(entrada.plusMinutes(90), 5000));

        r.registrarSalida(entrada.plusMinutes(150), 5000);
        assertFalse(r.estaActivo());
        assertEquals(15000.0, r.getCobro());
        assertEquals(150, r.minutosEstacionado(r.getSalida()));
    }

    @Test
    void testUtilidades() {
        assertEquals("ABC123", PlacaUtils.normalizar(" abc-123 "));
        assertEquals("ABC12D", PlacaUtils.normalizar("abc 12d"));
        assertNull(PlacaUtils.normalizar(null));

        assertEquals(0.3, CalculoUtils.redondear(0.1 + 0.2));
        assertEquals(33.33, CalculoUtils.promedio(100, 3));
        assertEquals(0.0, CalculoUtils.promedio(100, 0));
    }

    private Pedido pedidoCon(EstadoPedido estado, double precio, int cantidad) {
        return Pedido.builder().id((long) (Math.random() * 1000)).estado(estado).items(List.of(
                ItemPedido.builder().precioCongelado(precio).cantidad(cantidad).build())).build();
    }

    @Test
    void testCuentaTotalIgnoraCancelados() {
        Cuenta cuenta = Cuenta.builder().estado(EstadoCuenta.ABIERTA).pedidos(List.of(
                pedidoCon(EstadoPedido.ENTREGADO, 10000.0, 2),
                pedidoCon(EstadoPedido.CANCELADO, 50000.0, 1),
                pedidoCon(EstadoPedido.LISTO, 5000.0, 1))).build();

        assertEquals(25000.0, cuenta.calcularTotal());
        assertEquals(1, cuenta.pedidosPendientes().size()); // el LISTO aún no se entrega
        assertEquals(0.0, Cuenta.builder().pedidos(null).build().calcularTotal());
        assertTrue(Cuenta.builder().pedidos(null).build().pedidosPendientes().isEmpty());
    }

    @Test
    void testCuentaCicloDePago() {
        Cuenta cuenta = Cuenta.builder().estado(EstadoCuenta.ABIERTA)
                .pedidos(List.of(pedidoCon(EstadoPedido.ENTREGADO, 20000.0, 2))).build();
        assertNull(cuenta.calcularCambio());

        cuenta.solicitarPago();
        assertEquals(EstadoCuenta.EN_PAGO, cuenta.getEstado());

        cuenta.pagar(MetodoPago.EFECTIVO, 50000.0, LocalDateTime.now());
        assertTrue(cuenta.estaCerrada());
        assertEquals(40000.0, cuenta.getTotal());
        assertEquals(10000.0, cuenta.calcularCambio());
        assertNotNull(cuenta.getFechaCierre());

        // Cerrada: el total queda congelado aunque cambie la lista
        cuenta.setPedidos(List.of());
        assertEquals(40000.0, cuenta.calcularTotal());
    }

    @Test
    void testEstadoPedidoFinal() {
        assertTrue(EstadoPedido.ENTREGADO.esFinal());
        assertTrue(EstadoPedido.CANCELADO.esFinal());
        assertFalse(EstadoPedido.LISTO.esFinal());
    }

    @Test
    void testItemPedidoSubtotal() {
        ItemPedido item = ItemPedido.builder().precioCongelado(12000.0).cantidad(3).build();
        assertEquals(36000.0, item.subtotal());

        ItemPedido itemInvalido = ItemPedido.builder().precioCongelado(null).cantidad(2).build();
        assertEquals(0.0, itemInvalido.subtotal());
    }
}