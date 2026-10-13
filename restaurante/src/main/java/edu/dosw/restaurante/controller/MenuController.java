package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.controller.docs.MenuApi;
import edu.dosw.restaurante.mapper.PlatoMapper;
import edu.dosw.restaurante.model.dto.response.PlatoResponseDTO;
import edu.dosw.restaurante.service.IPlatoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
public class MenuController implements MenuApi {

    // Mismo servicio que PlatoController: el menú es la vista de solo lectura del cliente
    private final IPlatoService platoService;
    private final PlatoMapper platoMapper;

    @Override
    @GetMapping
    public ResponseEntity<List<PlatoResponseDTO>> verCarta() {
        return ResponseEntity.ok(platoMapper.toResponseList(platoService.obtenerDisponibles()));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<PlatoResponseDTO> verDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(platoMapper.toResponse(platoService.obtenerDisponiblePorId(id)));
    }

    @Override
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<PlatoResponseDTO>> porCategoria(@PathVariable String categoria) {
        return ResponseEntity.ok(platoMapper.toResponseList(platoService.obtenerDisponiblesPorCategoria(categoria)));
    }
}
