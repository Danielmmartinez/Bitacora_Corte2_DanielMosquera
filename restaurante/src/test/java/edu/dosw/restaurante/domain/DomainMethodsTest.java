package edu.dosw.restaurante.domain;

import edu.dosw.restaurante.model.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DomainMethodsTest {

    @Test
    void testPlatoEsValido() {
        Plato platoValido = Plato.builder().nombre("Hamburguesa").precio(15000.0).build();
        assertTrue(platoValido.esValido());

        Plato platoInvalidoNombre = Plato.builder().nombre(" ").precio(15000.0).build();
        assertFalse(platoInvalidoNombre.esValido());

        Plato platoInvalidoPrecio = Plato.builder().nombre("Pizza").precio(0.0).build();
        assertFalse(platoInvalidoPrecio.esValido());
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

        mesa.abrirCuenta();
        assertEquals(EstadoMesa.OCUPADA, mesa.getEstado());
        assertTrue(mesa.getCuentaAbierta());

        mesa.cerrarCuenta();
        assertEquals(EstadoMesa.DISPONIBLE, mesa.getEstado());
        assertFalse(mesa.getCuentaAbierta());
    }

    @Test
    void testReservaMetodos() {
        Reserva reserva = Reserva.builder().fechaHora(LocalDateTime.now().plusDays(1)).build();
        assertTrue(reserva.estaVigente());

        reserva.cancelar();
        assertNull(reserva.getFechaHora());
        assertFalse(reserva.estaVigente());

        LocalDateTime nuevaFecha = LocalDateTime.now().plusDays(2);
        reserva.reprogramar(nuevaFecha);
        assertEquals(nuevaFecha, reserva.getFechaHora());
    }

    @Test
    void testRegistroVehiculoMetodos() {
        LocalDateTime entrada = LocalDateTime.now().minusHours(3);
        LocalDateTime salida = LocalDateTime.now();
        RegistroVehiculo vehiculo = RegistroVehiculo.builder().entrada(entrada).salida(salida).build();

        assertEquals(15000.0, vehiculo.calcularCobro());
        assertFalse(vehiculo.estaActivo());

        RegistroVehiculo vehiculoActivo = RegistroVehiculo.builder().entrada(entrada).salida(null).build();
        assertTrue(vehiculoActivo.estaActivo());
        assertEquals(0.0, vehiculoActivo.calcularCobro());

        vehiculoActivo.registrarSalida();
        assertNotNull(vehiculoActivo.getSalida());
    }

    @Test
    void testCuentaMetodos() {
        Cuenta cuenta = Cuenta.builder().total(50000.0).estado(EstadoCuenta.ABIERTA).build();
        assertEquals(50000.0, cuenta.calcularTotal());

        cuenta.registrarPago();
        assertEquals(EstadoCuenta.EN_PAGO, cuenta.getEstado());

        cuenta.cerrarCuenta();
        assertEquals(EstadoCuenta.CERRADA, cuenta.getEstado());

        Cuenta cuentaNull = Cuenta.builder().total(null).build();
        assertEquals(0.0, cuentaNull.calcularTotal());
    }

    @Test
    void testItemPedidoSubtotal() {
        ItemPedido item = ItemPedido.builder().precioCongelado(12000.0).cantidad(3).build();
        assertEquals(36000.0, item.subtotal());

        ItemPedido itemInvalido = ItemPedido.builder().precioCongelado(null).cantidad(2).build();
        assertEquals(0.0, itemInvalido.subtotal());
    }
}