package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.mapper.PedidoMapper;
import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.domain.Pedido;
import edu.dosw.restaurante.model.dto.request.PedidoRequestDTO;
import edu.dosw.restaurante.model.dto.response.PedidoResponseDTO;
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

    @InjectMocks
    private PedidoController pedidoController;

    @Test
    void obtenerTodos_DevuelveOK() {
        when(pedidoService.obtenerTodos()).thenReturn(List.of(new Pedido()));
        when(pedidoMapper.toResponseList(any())).thenReturn(List.of(new PedidoResponseDTO()));

        ResponseEntity<List<PedidoResponseDTO>> response = pedidoController.obtenerTodos();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty());
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
    void eliminar_DevuelveNoContent() {
        doNothing().when(pedidoService).eliminar(1L);

        ResponseEntity<Void> response = pedidoController.eliminar(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}