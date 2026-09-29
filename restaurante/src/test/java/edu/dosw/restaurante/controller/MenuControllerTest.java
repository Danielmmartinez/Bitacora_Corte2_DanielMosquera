package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.mapper.PlatoMapper;
import edu.dosw.restaurante.model.domain.Plato;
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
class MenuControllerTest {

    @Mock
    private IPlatoService platoService;

    @Mock
    private PlatoMapper platoMapper;

    @InjectMocks
    private MenuController menuController;

    @Test
    void verCarta_SoloConsultaDisponibles() {
        when(platoService.obtenerDisponibles()).thenReturn(List.of(new Plato()));
        when(platoMapper.toResponseList(any())).thenReturn(List.of(new PlatoResponseDTO()));

        ResponseEntity<List<PlatoResponseDTO>> response = menuController.verCarta();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(platoService, never()).obtenerTodos();
    }

    @Test
    void verDetalle_DevuelveOK() {
        when(platoService.obtenerDisponiblePorId(1L)).thenReturn(new Plato());
        when(platoMapper.toResponse(any())).thenReturn(new PlatoResponseDTO());

        ResponseEntity<PlatoResponseDTO> response = menuController.verDetalle(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void porCategoria_DevuelveOK() {
        when(platoService.obtenerDisponiblesPorCategoria("Rolls")).thenReturn(List.of());
        when(platoMapper.toResponseList(any())).thenReturn(List.of());

        ResponseEntity<List<PlatoResponseDTO>> response = menuController.porCategoria("Rolls");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }
}
