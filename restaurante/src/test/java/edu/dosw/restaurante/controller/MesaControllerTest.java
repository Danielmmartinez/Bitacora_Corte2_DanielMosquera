package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.mapper.MesaMapper;
import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.model.dto.request.MesaRequestDTO;
import edu.dosw.restaurante.model.dto.response.MesaResponseDTO;
import edu.dosw.restaurante.service.IMesaService;
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
class MesaControllerTest {

    @Mock
    private IMesaService mesaService;

    @Mock
    private MesaMapper mesaMapper;

    @InjectMocks
    private MesaController mesaController;

    @Test
    void obtenerTodas_DevuelveOK() {
        when(mesaService.obtenerTodas()).thenReturn(List.of(new Mesa()));
        when(mesaMapper.toResponseList(any())).thenReturn(List.of(new MesaResponseDTO()));

        ResponseEntity<List<MesaResponseDTO>> response = mesaController.obtenerTodas();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    void obtenerPorId_DevuelveOK() {
        when(mesaService.obtenerPorId(1L)).thenReturn(new Mesa());
        when(mesaMapper.toResponse(any())).thenReturn(new MesaResponseDTO());

        ResponseEntity<MesaResponseDTO> response = mesaController.obtenerPorId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void crear_DevuelveCreated() {
        MesaRequestDTO requestDTO = new MesaRequestDTO(1, 4);
        when(mesaMapper.toDomain(any())).thenReturn(new Mesa());
        when(mesaService.crear(any())).thenReturn(new Mesa());
        when(mesaMapper.toResponse(any())).thenReturn(new MesaResponseDTO());

        ResponseEntity<MesaResponseDTO> response = mesaController.crear(requestDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void cambiarEstado_DevuelveOK() {
        when(mesaService.cambiarEstado(1L, EstadoMesa.OCUPADA)).thenReturn(new Mesa());
        when(mesaMapper.toResponse(any())).thenReturn(new MesaResponseDTO());

        ResponseEntity<MesaResponseDTO> response = mesaController.cambiarEstado(1L, EstadoMesa.OCUPADA);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void eliminar_DevuelveNoContent() {
        doNothing().when(mesaService).eliminar(1L);

        ResponseEntity<Void> response = mesaController.eliminar(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}