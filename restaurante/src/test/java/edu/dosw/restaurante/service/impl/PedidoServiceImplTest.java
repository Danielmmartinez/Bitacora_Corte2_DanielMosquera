package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.model.domain.*;
import edu.dosw.restaurante.service.IMesaService;
import edu.dosw.restaurante.service.IPlatoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock
    private IPlatoService platoService;

    @Mock
    private IMesaService mesaService;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    private Mesa mesaDummy;
    private Plato platoDummy;

    @BeforeEach
    void setUp() {
        mesaDummy = Mesa.builder().id(1L).numero(1).capacidad(4).estado(EstadoMesa.DISPONIBLE).build();
        platoDummy = Plato.builder().id(10L).nombre("Hamburguesa").precio(20000.0).disponible(true).build();
    }

    @Test
    void crearPedido_Exito() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaDummy);
        when(platoService.obtenerPorId(10L)).thenReturn(platoDummy);

        ItemPedido item = ItemPedido.builder().idPlato(10L).cantidad(2).build();
        List<ItemPedido> items = new ArrayList<>();
        items.add(item);

        Pedido pedido = Pedido.builder().idMesa(1L).items(items).build();

        Pedido creado = pedidoService.crear(pedido);

        assertNotNull(creado.getId());
        assertEquals(EstadoPedido.RECIBIDO, creado.getEstado());
        assertEquals(20000.0, creado.getItems().get(0).getPrecioCongelado());
        assertEquals("Hamburguesa", creado.getItems().get(0).getNombrePlato());
    }

    @Test
    void crearPedido_SinItems_LanzaEstadoInvalido() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaDummy);

        Pedido pedido = Pedido.builder().idMesa(1L).items(new ArrayList<>()).build();

        assertThrows(EstadoInvalidoException.class, () -> pedidoService.crear(pedido));
    }

    @Test
    void crearPedido_PlatoNoDisponible_LanzaEstadoInvalido() {
        platoDummy.setDisponible(false);
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaDummy);
        when(platoService.obtenerPorId(10L)).thenReturn(platoDummy);

        ItemPedido item = ItemPedido.builder().idPlato(10L).cantidad(1).build();
        Pedido pedido = Pedido.builder().idMesa(1L).items(List.of(item)).build();

        assertThrows(EstadoInvalidoException.class, () -> pedidoService.crear(pedido));
    }

    @Test
    void cambiarEstado_Exito() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaDummy);
        when(platoService.obtenerPorId(10L)).thenReturn(platoDummy);

        ItemPedido item = ItemPedido.builder().idPlato(10L).cantidad(1).build();
        Pedido pedido = pedidoService.crear(Pedido.builder().idMesa(1L).items(List.of(item)).build());

        Pedido actualizado = pedidoService.cambiarEstado(pedido.getId(), EstadoPedido.EN_PREPARACION);
        assertEquals(EstadoPedido.EN_PREPARACION, actualizado.getEstado());
    }

    @Test
    void obtenerPorMesa_Exito() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaDummy);
        List<Pedido> pedidos = pedidoService.obtenerPorMesa(1L);
        assertNotNull(pedidos);
    }

    @Test
    void eliminarPedido_Exito() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesaDummy);
        when(platoService.obtenerPorId(10L)).thenReturn(platoDummy);

        ItemPedido item = ItemPedido.builder().idPlato(10L).cantidad(1).build();
        Pedido pedido = pedidoService.crear(Pedido.builder().idMesa(1L).items(List.of(item)).build());

        Long id = pedido.getId();
        pedidoService.eliminar(id);
        assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.obtenerPorId(id));
    }
}