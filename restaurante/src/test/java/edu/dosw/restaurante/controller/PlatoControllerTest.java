package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.mapper.PlatoMapper;
import edu.dosw.restaurante.model.domain.Plato;
import edu.dosw.restaurante.model.dto.request.PlatoRequestDTO;
import edu.dosw.restaurante.model.dto.response.PlatoResponseDTO;
import edu.dosw.restaurante.service.IPlatoService;
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
class PlatoControllerTest {

    @Mock
    private IPlatoService platoService;

    @Mock
    private PlatoMapper platoMapper;

    @InjectMocks
    private PlatoController platoController;

    @Test
    void obtenerTodos_DevuelveOK() {
        when(platoService.obtenerTodos()).thenReturn(List.of(new Plato()));
        when(platoMapper.toResponseList(any())).thenReturn(List.of(new PlatoResponseDTO()));

        ResponseEntity<List<PlatoResponseDTO>> response = platoController.obtenerTodos();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    void obtenerPorId_DevuelveOK() {
        when(platoService.obtenerPorId(1L)).thenReturn(new Plato());
        when(platoMapper.toResponse(any())).thenReturn(new PlatoResponseDTO());

        ResponseEntity<PlatoResponseDTO> response = platoController.obtenerPorId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void crear_DevuelveCreated() {
        PlatoRequestDTO requestDTO = new PlatoRequestDTO("Sopa", 5000.0, "Entrada", true);
        when(platoMapper.toDomain(any())).thenReturn(new Plato());
        when(platoService.crear(any())).thenReturn(new Plato());
        when(platoMapper.toResponse(any())).thenReturn(new PlatoResponseDTO());

        ResponseEntity<PlatoResponseDTO> response = platoController.crear(requestDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void eliminar_DevuelveNoContent() {
        doNothing().when(platoService).eliminar(1L);

        ResponseEntity<Void> response = platoController.eliminar(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}