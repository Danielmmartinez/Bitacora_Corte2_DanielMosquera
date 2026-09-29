package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.mapper.CuentaEntityMapper;
import edu.dosw.restaurante.mapper.CuentaEntityMapperImpl;
import edu.dosw.restaurante.model.domain.*;
import edu.dosw.restaurante.persistence.entity.CuentaEntity;
import edu.dosw.restaurante.repository.CuentaRepository;
import edu.dosw.restaurante.service.IMesaService;
import edu.dosw.restaurante.service.IPedidoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Spy
    private CuentaEntityMapper entityMapper = new CuentaEntityMapperImpl();

    @Mock
    private IMesaService mesaService;

    @Mock
    private IPedidoService pedidoService;

    // Reloj real: estas pruebas no dependen de una hora concreta
    @Spy
    private Clock clock = Clock.systemDefaultZone();

    @InjectMocks
    private CuentaServiceImpl cuentaService;

    private Mesa mesaLibre() {
        return Mesa.builder().id(1L).numero(1).estado(EstadoMesa.DISPONIBLE).cuentaAbierta(false).build();
    }

    private CuentaEntity cuentaEntity(Long id, EstadoCuenta estado) {
        return CuentaEntity.builder().id(id).idMesa(1L).estado(estado).fechaApertura(LocalDateTime.now()).build();
    }

    private Pedido pedido(EstadoPedido estado, double precio, int cantidad) {
        return Pedido.builder().id(10L).idCuenta(5L).estado(estado).items(List.of(
                ItemPedido.builder().precioCongelado(precio).cantidad(cantidad).build())).build();
    }

    private void saveDevuelveLoMismo() {
        when(cuentaRepository.save(any())).thenAnswer(inv -> {
            CuentaEntity e = inv.getArgument(0);
            if (e.getId() == null) e.setId(5L);
            return e;
        });
    }

    private CuentaEntity capturarGuardada() {
        ArgumentCaptor<CuentaEntity> captor = ArgumentCaptor.forClass(CuentaEntity.class);
        verify(cuentaRepository).save(captor.capture());
        return captor.getValue();
    }

    // ── abrir ───────────────────────────────────────────────────

    @Test
    void abrir_Exito_CreaCuentaYOcupaLaMesa() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaLibre());
        saveDevuelveLoMismo();

        Cuenta cuenta = cuentaService.abrir(1L);

        assertEquals(5L, cuenta.getId());
        assertEquals(EstadoCuenta.ABIERTA, cuenta.getEstado());
        assertNotNull(cuenta.getFechaApertura());
        verify(mesaService).ocupar(1L, 5L);
    }

    @Test
    void abrir_MesaConCuentaAbierta_LanzaConflicto() {
        Mesa ocupada = Mesa.builder().id(1L).numero(1).cuentaAbierta(true).idCuentaAbierta(4L).build();
        when(mesaService.obtenerPorId(1L)).thenReturn(ocupada);

        assertThrows(ConflictoException.class, () -> cuentaService.abrir(1L));
        verify(cuentaRepository, never()).save(any());
        verify(mesaService, never()).ocupar(anyLong(), anyLong());
    }

    @Test
    void abrir_MesaNoExiste_PropagaNoEncontrado() {
        when(mesaService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe"));
        assertThrows(RecursoNoEncontradoException.class, () -> cuentaService.abrir(99L));
    }

    // ── consultas ───────────────────────────────────────────────

    @Test
    void obtenerPorId_CargaLosPedidosYCalculaTotal() {
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(cuentaEntity(5L, EstadoCuenta.ABIERTA)));
        when(pedidoService.obtenerPorCuenta(5L)).thenReturn(List.of(
                pedido(EstadoPedido.ENTREGADO, 10000.0, 3), pedido(EstadoPedido.CANCELADO, 99999.0, 1)));

        Cuenta cuenta = cuentaService.obtenerPorId(5L);

        assertEquals(2, cuenta.getPedidos().size());
        assertEquals(30000.0, cuenta.calcularTotal());
    }

    @Test
    void obtenerPorId_NoExiste_LanzaExcepcion() {
        when(cuentaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> cuentaService.obtenerPorId(99L));
    }

    @Test
    void obtenerTodas_Y_PorEstado() {
        when(cuentaRepository.findAll()).thenReturn(List.of(cuentaEntity(1L, EstadoCuenta.ABIERTA)));
        when(cuentaRepository.findByEstado(EstadoCuenta.CERRADA)).thenReturn(List.of());

        assertEquals(1, cuentaService.obtenerTodas().size());
        assertTrue(cuentaService.obtenerPorEstado(EstadoCuenta.CERRADA).isEmpty());
    }

    @Test
    void obtenerAbiertaPorMesa_Existe() {
        Mesa ocupada = Mesa.builder().id(1L).numero(1).cuentaAbierta(true).idCuentaAbierta(5L).build();
        when(mesaService.obtenerPorId(1L)).thenReturn(ocupada);
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(cuentaEntity(5L, EstadoCuenta.ABIERTA)));

        assertEquals(5L, cuentaService.obtenerAbiertaPorMesa(1L).getId());
    }

    @Test
    void obtenerAbiertaPorMesa_SinCuenta_LanzaNoEncontrado() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaLibre());
        assertThrows(RecursoNoEncontradoException.class, () -> cuentaService.obtenerAbiertaPorMesa(1L));
    }

    // ── solicitar ───────────────────────────────────────────────

    @Test
    void solicitarPago_Abierta_PasaAEnPago() {
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(cuentaEntity(5L, EstadoCuenta.ABIERTA)));
        saveDevuelveLoMismo();

        assertEquals(EstadoCuenta.EN_PAGO, cuentaService.solicitarPago(5L).getEstado());
    }

    @Test
    void solicitarPago_NoAbierta_LanzaEstadoInvalido() {
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(cuentaEntity(5L, EstadoCuenta.EN_PAGO)));

        assertThrows(EstadoInvalidoException.class, () -> cuentaService.solicitarPago(5L));
        verify(cuentaRepository, never()).save(any());
    }

    // ── pagar ───────────────────────────────────────────────────

    @Test
    void pagar_Exito_CongelaTotalCierraYLiberaLaMesa() {
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(cuentaEntity(5L, EstadoCuenta.EN_PAGO)));
        when(pedidoService.obtenerPorCuenta(5L)).thenReturn(List.of(pedido(EstadoPedido.ENTREGADO, 22000.0, 2)));
        saveDevuelveLoMismo();

        Cuenta pagada = cuentaService.pagar(5L, MetodoPago.EFECTIVO, 50000.0);

        assertEquals(EstadoCuenta.CERRADA, pagada.getEstado());
        assertEquals(44000.0, pagada.getTotal());
        assertEquals(6000.0, pagada.calcularCambio());
        CuentaEntity guardada = capturarGuardada();
        assertEquals(44000.0, guardada.getTotal());
        assertEquals(MetodoPago.EFECTIVO, guardada.getMetodoPago());
        assertNotNull(guardada.getFechaCierre());
        verify(mesaService).liberar(1L);
    }

    @Test
    void pagar_DesdeAbierta_TambienSePermite() {
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(cuentaEntity(5L, EstadoCuenta.ABIERTA)));
        when(pedidoService.obtenerPorCuenta(5L)).thenReturn(List.of());
        saveDevuelveLoMismo();

        Cuenta pagada = cuentaService.pagar(5L, MetodoPago.TARJETA, 0.0);

        assertEquals(0.0, pagada.getTotal()); // mesa que se fue sin pedir
    }

    @Test
    void pagar_ConPedidosSinEntregar_LanzaEstadoInvalido() {
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(cuentaEntity(5L, EstadoCuenta.EN_PAGO)));
        when(pedidoService.obtenerPorCuenta(5L)).thenReturn(List.of(pedido(EstadoPedido.EN_PREPARACION, 10000.0, 1)));

        EstadoInvalidoException ex = assertThrows(EstadoInvalidoException.class,
                () -> cuentaService.pagar(5L, MetodoPago.EFECTIVO, 100000.0));
        assertTrue(ex.getMessage().contains("EN_PREPARACION"));
        verify(cuentaRepository, never()).save(any());
        verify(mesaService, never()).liberar(anyLong());
    }

    @Test
    void pagar_MontoInsuficiente_LanzaEstadoInvalido() {
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(cuentaEntity(5L, EstadoCuenta.EN_PAGO)));
        when(pedidoService.obtenerPorCuenta(5L)).thenReturn(List.of(pedido(EstadoPedido.ENTREGADO, 30000.0, 1)));

        assertThrows(EstadoInvalidoException.class, () -> cuentaService.pagar(5L, MetodoPago.EFECTIVO, 20000.0));
        verify(mesaService, never()).liberar(anyLong());
    }

    @Test
    void pagar_YaCerrada_LanzaEstadoInvalido() {
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(cuentaEntity(5L, EstadoCuenta.CERRADA)));

        assertThrows(EstadoInvalidoException.class, () -> cuentaService.pagar(5L, MetodoPago.EFECTIVO, 1.0));
        verify(cuentaRepository, never()).save(any());
    }
}
