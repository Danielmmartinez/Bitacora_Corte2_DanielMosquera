package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.mapper.EventoPedidoMapper;
import edu.dosw.restaurante.mapper.PedidoMapper;
import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.domain.EventoPedido;
import edu.dosw.restaurante.model.domain.Pedido;
import edu.dosw.restaurante.model.dto.request.PedidoRequestDTO;
import edu.dosw.restaurante.model.dto.response.EventoPedidoResponseDTO;
import edu.dosw.restaurante.model.dto.response.PedidoResponseDTO;
import edu.dosw.restaurante.service.IEventoPedidoService;
import edu.dosw.restaurante.service.IPedidoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private IPedidoService pedidoService;

    @Mock
    private PedidoMapper pedidoMapper;

    @Mock
    private IEventoPedidoService eventoService;

    @Mock
    private EventoPedidoMapper eventoMapper;

    @InjectMocks
    private PedidoController pedidoController;

    @Test
    void obtenerTodos_SinFiltro_DevuelveOK() {
        when(pedidoService.obtenerTodos()).thenReturn(List.of(new Pedido()));
        when(pedidoMapper.toResponseList(any())).thenReturn(List.of(new PedidoResponseDTO()));

        ResponseEntity<List<PedidoResponseDTO>> response = pedidoController.obtenerTodos(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty());
        verify(pedidoService, never()).obtenerPorEstado(any());
    }

    @Test
    void obtenerTodos_ConFiltro_UsaObtenerPorEstado() {
        when(pedidoService.obtenerPorEstado(EstadoPedido.RECIBIDO)).thenReturn(List.of(new Pedido()));
        when(pedidoMapper.toResponseList(any())).thenReturn(List.of(new PedidoResponseDTO()));

        ResponseEntity<List<PedidoResponseDTO>> response = pedidoController.obtenerTodos(EstadoPedido.RECIBIDO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(pedidoService, never()).obtenerTodos();
    }

    @Test
    void actualizar_DevuelveOK() {
        PedidoRequestDTO requestDTO = new PedidoRequestDTO(1L, List.of());
        Pedido dominio = new Pedido();
        when(pedidoMapper.toDomain(requestDTO)).thenReturn(dominio);
        when(pedidoService.actualizar(1L, dominio)).thenReturn(dominio);
        when(pedidoMapper.toResponse(dominio)).thenReturn(new PedidoResponseDTO());

        ResponseEntity<PedidoResponseDTO> response = pedidoController.actualizar(1L, requestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void obtenerPorId_DevuelveOK() {
        when(pedidoService.obtenerPorId(1L)).thenReturn(new Pedido());
        when(pedidoMapper.toResponse(any())).thenReturn(new PedidoResponseDTO());

        ResponseEntity<PedidoResponseDTO> response = pedidoController.obtenerPorId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void obtenerPorMesa_DevuelveOK() {
        when(pedidoService.obtenerPorMesa(1L)).thenReturn(List.of(new Pedido()));
        when(pedidoMapper.toResponseList(any())).thenReturn(List.of(new PedidoResponseDTO()));

        ResponseEntity<List<PedidoResponseDTO>> response = pedidoController.obtenerPorMesa(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void crear_DevuelveCreated() {
        PedidoRequestDTO requestDTO = new PedidoRequestDTO(1L, List.of());
        when(pedidoMapper.toDomain(any())).thenReturn(new Pedido());
        when(pedidoService.crear(any())).thenReturn(new Pedido());
        when(pedidoMapper.toResponse(any())).thenReturn(new PedidoResponseDTO());

        ResponseEntity<PedidoResponseDTO> response = pedidoController.crear(requestDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void cambiarEstado_DevuelveOK() {
        when(pedidoService.cambiarEstado(1L, EstadoPedido.EN_PREPARACION)).thenReturn(new Pedido());
        when(pedidoMapper.toResponse(any())).thenReturn(new PedidoResponseDTO());

        ResponseEntity<PedidoResponseDTO> response = pedidoController.cambiarEstado(1L, EstadoPedido.EN_PREPARACION);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void obtenerHistorial_DevuelveOK() {
        when(eventoService.obtenerHistorial(1L)).thenReturn(List.of(new EventoPedido()));
        when(eventoMapper.toResponseList(any())).thenReturn(List.of(new EventoPedidoResponseDTO()));

        ResponseEntity<List<EventoPedidoResponseDTO>> response = pedidoController.obtenerHistorial(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void eliminar_DevuelveNoContent() {
        doNothing().when(pedidoService).eliminar(1L);

        ResponseEntity<Void> response = pedidoController.eliminar(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}