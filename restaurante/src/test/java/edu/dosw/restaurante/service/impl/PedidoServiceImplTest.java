package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.mapper.PedidoEntityMapper;
import edu.dosw.restaurante.mapper.PedidoEntityMapperImpl;
import edu.dosw.restaurante.model.domain.*;
import edu.dosw.restaurante.persistence.entity.ItemPedidoEntity;
import edu.dosw.restaurante.persistence.entity.PedidoEntity;
import edu.dosw.restaurante.repository.PedidoRepository;
import edu.dosw.restaurante.service.IEventoPedidoService;
import edu.dosw.restaurante.service.IMesaService;
import edu.dosw.restaurante.service.IPlatoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Spy
    private PedidoEntityMapper entityMapper = new PedidoEntityMapperImpl();

    @Mock
    private IPlatoService platoService;

    @Mock
    private IMesaService mesaService;

    @Mock
    private IEventoPedidoService eventoService;

    // Reloj real: estas pruebas no dependen de una hora concreta
    @Spy
    private Clock clock = Clock.systemDefaultZone();

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    private Mesa mesaAbierta;
    private Plato platoDummy;

    @BeforeEach
    void setUp() {
        mesaAbierta = Mesa.builder().id(1L).numero(1).capacidad(4)
                .estado(EstadoMesa.OCUPADA).cuentaAbierta(true).idCuentaAbierta(3L).build();
        platoDummy = Plato.builder().id(10L).nombre("Hamburguesa").precio(20000.0).disponible(true).build();
    }

    private Pedido nuevoPedido(Long idMesa, int cantidad) {
        List<ItemPedido> items = new ArrayList<>();
        items.add(ItemPedido.builder().idPlato(10L).cantidad(cantidad).build());
        return Pedido.builder().idMesa(idMesa).items(items).build();
    }

    private PedidoEntity entidadGuardada(Long id, EstadoPedido estado) {
        List<ItemPedidoEntity> items = new ArrayList<>();
        items.add(ItemPedidoEntity.builder().id(100L).idPlato(10L).nombrePlato("Hamburguesa")
                .precioCongelado(20000.0).cantidad(1).build());
        return PedidoEntity.builder().id(id).idMesa(1L).estado(estado)
                .timestamp(LocalDateTime.now()).items(items).build();
    }

    // Simula a la BD: asigna IDs al pedido y a los ítems nuevos
    private final List<Long> idsRecibidosEnSave = new ArrayList<>();

    private void saveAsignaIds() {
        AtomicLong seq = new AtomicLong(1);
        when(pedidoRepository.save(any())).thenAnswer(inv -> {
            PedidoEntity e = inv.getArgument(0);
            idsRecibidosEnSave.add(e.getId());
            if (e.getId() == null) e.setId(seq.getAndIncrement());
            e.getItems().forEach(i -> { if (i.getId() == null) i.setId(seq.getAndIncrement() + 100); });
            return e;
        });
    }

    private EventoPedido capturarEvento() {
        ArgumentCaptor<EventoPedido> captor = ArgumentCaptor.forClass(EventoPedido.class);
        verify(eventoService).registrar(captor.capture());
        return captor.getValue();
    }

    private PedidoEntity capturarGuardado() {
        ArgumentCaptor<PedidoEntity> captor = ArgumentCaptor.forClass(PedidoEntity.class);
        verify(pedidoRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void crear_Exito_CongelaPrecioYGuardaItemsEnCascada() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaAbierta);
        when(platoService.obtenerPorId(10L)).thenReturn(platoDummy);
        saveAsignaIds();

        Pedido creado = pedidoService.crear(nuevoPedido(1L, 2));

        assertNotNull(creado.getId());
        assertNotNull(creado.getTimestamp());
        assertEquals(EstadoPedido.RECIBIDO, creado.getEstado());
        assertEquals(3L, creado.getIdCuenta()); // se cobra en la cuenta abierta de la mesa
        assertEquals(20000.0, creado.getItems().get(0).getPrecioCongelado());
        assertEquals("Hamburguesa", creado.getItems().get(0).getNombrePlato());
        assertNotNull(creado.getItems().get(0).getId());
        assertEquals(40000.0, creado.calcularTotal());

        assertNull(idsRecibidosEnSave.get(0));
        assertEquals(1, capturarGuardado().getItems().size());

        EventoPedido evento = capturarEvento();
        assertEquals(TipoEventoPedido.CREADO, evento.getTipo());
        assertEquals(creado.getId(), evento.getIdPedido());
        assertNull(evento.getEstadoAnterior());
        assertEquals(EstadoPedido.RECIBIDO, evento.getEstadoNuevo());
        assertEquals(40000.0, evento.getDetalle().get("total"));
        assertEquals(1, ((List<?>) evento.getDetalle().get("items")).size());
    }

    @Test
    void crear_MesaSinCuentaAbierta_LanzaEstadoInvalido() {
        Mesa mesaLibre = Mesa.builder().id(1L).numero(1).estado(EstadoMesa.DISPONIBLE).cuentaAbierta(false).build();
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaLibre);

        Pedido pedido = nuevoPedido(1L, 1);
        assertThrows(EstadoInvalidoException.class, () -> pedidoService.crear(pedido));
        verifyNoInteractions(platoService);
        verify(pedidoRepository, never()).save(any());
        verifyNoInteractions(eventoService); // si falla, no queda evento en el historial
    }

    @Test
    void crear_MesaNoExiste_PropagaNoEncontrado() {
        when(mesaService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe"));

        Pedido pedido = nuevoPedido(99L, 1);
        assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.crear(pedido));
    }

    @Test
    void crear_SinItems_LanzaEstadoInvalido() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaAbierta);

        Pedido pedido = Pedido.builder().idMesa(1L).items(new ArrayList<>()).build();

        assertThrows(EstadoInvalidoException.class, () -> pedidoService.crear(pedido));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crear_PlatoNoDisponible_LanzaEstadoInvalido() {
        platoDummy.setDisponible(false);
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaAbierta);
        when(platoService.obtenerPorId(10L)).thenReturn(platoDummy);

        Pedido pedido = nuevoPedido(1L, 1);
        assertThrows(EstadoInvalidoException.class, () -> pedidoService.crear(pedido));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void obtenerPorId_Existe_ConvierteConItems() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(entidadGuardada(5L, EstadoPedido.RECIBIDO)));

        Pedido pedido = pedidoService.obtenerPorId(5L);

        assertEquals(5L, pedido.getId());
        assertEquals(1, pedido.getItems().size());
        assertEquals(20000.0, pedido.calcularTotal());
    }

    @Test
    void obtenerPorId_NoExiste_LanzaExcepcion() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.obtenerPorId(99L));
    }

    @Test
    void obtenerTodos_SinPedidos_DevuelveListaVacia() {
        when(pedidoRepository.findAll()).thenReturn(List.of());
        assertTrue(pedidoService.obtenerTodos().isEmpty());
    }

    @Test
    void obtenerPorEstado_DelegaAlRepositorio() {
        when(pedidoRepository.findByEstado(EstadoPedido.EN_PREPARACION))
                .thenReturn(List.of(entidadGuardada(1L, EstadoPedido.EN_PREPARACION)));

        List<Pedido> enCocina = pedidoService.obtenerPorEstado(EstadoPedido.EN_PREPARACION);

        assertEquals(1, enCocina.size());
        assertEquals(EstadoPedido.EN_PREPARACION, enCocina.get(0).getEstado());
    }

    @Test
    void obtenerPorMesa_ValidaMesaYDelega() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaAbierta);
        when(pedidoRepository.findByIdMesa(1L)).thenReturn(List.of(entidadGuardada(1L, EstadoPedido.RECIBIDO)));

        List<Pedido> pedidos = pedidoService.obtenerPorMesa(1L);

        assertEquals(1, pedidos.size());
        assertEquals(1L, pedidos.get(0).getIdMesa());
    }

    @Test
    void obtenerPorCuenta_DelegaAlRepositorio() {
        PedidoEntity entidad = entidadGuardada(1L, EstadoPedido.ENTREGADO);
        entidad.setIdCuenta(3L);
        when(pedidoRepository.findByIdCuenta(3L)).thenReturn(List.of(entidad));

        List<Pedido> pedidos = pedidoService.obtenerPorCuenta(3L);

        assertEquals(1, pedidos.size());
        assertEquals(3L, pedidos.get(0).getIdCuenta());
    }

    @Test
    void obtenerPorMesa_MesaNoExiste_LanzaExcepcion() {
        when(mesaService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe"));

        assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.obtenerPorMesa(99L));
        verify(pedidoRepository, never()).findByIdMesa(any());
    }

    @Test
    void cambiarEstado_TransicionValida_Guarda() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(entidadGuardada(1L, EstadoPedido.RECIBIDO)));
        saveAsignaIds();

        Pedido actualizado = pedidoService.cambiarEstado(1L, EstadoPedido.EN_PREPARACION);

        assertEquals(EstadoPedido.EN_PREPARACION, actualizado.getEstado());
        assertEquals(EstadoPedido.EN_PREPARACION, capturarGuardado().getEstado());

        EventoPedido evento = capturarEvento();
        assertEquals(TipoEventoPedido.CAMBIO_ESTADO, evento.getTipo());
        assertEquals(EstadoPedido.RECIBIDO, evento.getEstadoAnterior());
        assertEquals(EstadoPedido.EN_PREPARACION, evento.getEstadoNuevo());
    }

    @Test
    void cambiarEstado_Cancelar_DesdeRecibido() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(entidadGuardada(1L, EstadoPedido.RECIBIDO)));
        saveAsignaIds();

        assertEquals(EstadoPedido.CANCELADO, pedidoService.cambiarEstado(1L, EstadoPedido.CANCELADO).getEstado());
    }

    @Test
    void cambiarEstado_TransicionInvalida_LanzaEstadoInvalidoYNoGuarda() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(entidadGuardada(1L, EstadoPedido.EN_PREPARACION)));

        assertThrows(EstadoInvalidoException.class, () -> pedidoService.cambiarEstado(1L, EstadoPedido.CANCELADO));
        assertThrows(EstadoInvalidoException.class, () -> pedidoService.cambiarEstado(1L, EstadoPedido.RECIBIDO));
        verify(pedidoRepository, never()).save(any());
        verifyNoInteractions(eventoService);
    }

    @Test
    void cambiarEstado_NoExiste_LanzaExcepcion() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class,
                () -> pedidoService.cambiarEstado(99L, EstadoPedido.EN_PREPARACION));
    }

    @Test
    void actualizar_EnRecibido_ReemplazaItemsYConservaId() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(entidadGuardada(1L, EstadoPedido.RECIBIDO)));
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaAbierta);
        when(platoService.obtenerPorId(10L)).thenReturn(platoDummy);
        saveAsignaIds();

        Pedido actualizado = pedidoService.actualizar(1L, nuevoPedido(1L, 5));

        assertEquals(1L, actualizado.getId());
        assertEquals(5, actualizado.getItems().get(0).getCantidad());
        assertEquals(100000.0, actualizado.calcularTotal());
        assertEquals(EstadoPedido.RECIBIDO, actualizado.getEstado());

        PedidoEntity guardado = capturarGuardado();
        assertEquals(1L, guardado.getId());
        assertEquals(3L, guardado.getIdCuenta());
        assertEquals(1, guardado.getItems().size());
        assertNotEquals(100L, guardado.getItems().get(0).getId()); // el ítem viejo ya no está

        EventoPedido evento = capturarEvento();
        assertEquals(TipoEventoPedido.MODIFICADO, evento.getTipo());
        assertEquals(20000.0, evento.getDetalle().get("totalAnterior"));
        assertEquals(100000.0, evento.getDetalle().get("total"));
    }

    @Test
    void actualizar_YaEnCocina_LanzaEstadoInvalido() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(entidadGuardada(1L, EstadoPedido.EN_PREPARACION)));

        Pedido cambios = nuevoPedido(1L, 3);
        assertThrows(EstadoInvalidoException.class, () -> pedidoService.actualizar(1L, cambios));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void actualizar_NoExiste_LanzaExcepcion() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        Pedido cambios = nuevoPedido(1L, 1);
        assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.actualizar(99L, cambios));
        verifyNoInteractions(platoService);
    }

    @Test
    void eliminar_EnRecibido_BorraEnLaBD() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(entidadGuardada(1L, EstadoPedido.RECIBIDO)));

        pedidoService.eliminar(1L);

        verify(pedidoRepository).deleteById(1L);
        EventoPedido evento = capturarEvento();
        assertEquals(TipoEventoPedido.ELIMINADO, evento.getTipo());
        assertEquals(EstadoPedido.RECIBIDO, evento.getEstadoAnterior());
        assertNull(evento.getEstadoNuevo());
    }

    @Test
    void eliminar_YaEnCocina_LanzaEstadoInvalido() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(entidadGuardada(1L, EstadoPedido.EN_PREPARACION)));

        assertThrows(EstadoInvalidoException.class, () -> pedidoService.eliminar(1L));
        verify(pedidoRepository, never()).deleteById(any());
        verifyNoInteractions(eventoService);
    }

    @Test
    void eliminar_NoExiste_LanzaExcepcion() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.eliminar(99L));
    }
}
