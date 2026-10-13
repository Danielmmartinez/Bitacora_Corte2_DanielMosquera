package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.controller.docs.PlatoApi;
import edu.dosw.restaurante.mapper.PlatoMapper;
import edu.dosw.restaurante.model.domain.Plato;
import edu.dosw.restaurante.model.dto.request.PlatoRequestDTO;
import edu.dosw.restaurante.model.dto.response.PlatoResponseDTO;
import edu.dosw.restaurante.service.IPlatoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platos")
@RequiredArgsConstructor
public class PlatoController implements PlatoApi {

    private final IPlatoService platoService;
    private final PlatoMapper platoMapper;

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO', 'COCINERO')")
    public ResponseEntity<List<PlatoResponseDTO>> obtenerTodos() {
        List<Plato> platos = platoService.obtenerTodos();
        return ResponseEntity.ok(platoMapper.toResponseList(platos));
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO', 'COCINERO')")
    public ResponseEntity<PlatoResponseDTO> obtenerPorId(@PathVariable Long id) {
        Plato plato = platoService.obtenerPorId(id);
        return ResponseEntity.ok(platoMapper.toResponse(plato));
    }

    @Override
    @PostMapping
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<PlatoResponseDTO> crear(@Valid @RequestBody PlatoRequestDTO request) {
        Plato plato = platoMapper.toDomain(request);
        Plato creado = platoService.crear(plato);
        return ResponseEntity.status(HttpStatus.CREATED).body(platoMapper.toResponse(creado));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<PlatoResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody PlatoRequestDTO request) {
        Plato plato = platoMapper.toDomain(request);
        Plato actualizado = platoService.actualizar(id, plato);
        return ResponseEntity.ok(platoMapper.toResponse(actualizado));
    }

    @Override
    @PatchMapping("/{id}/disponible")
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO', 'COCINERO')")
    public ResponseEntity<PlatoResponseDTO> cambiarDisponibilidad(@PathVariable Long id, @RequestParam boolean disponible) {
        Plato actualizado = platoService.cambiarDisponibilidad(id, disponible);
        return ResponseEntity.ok(platoMapper.toResponse(actualizado));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        platoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
