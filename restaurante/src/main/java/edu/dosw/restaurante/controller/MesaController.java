package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.controller.docs.MesaApi;
import edu.dosw.restaurante.mapper.MesaMapper;
import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.model.dto.request.MesaRequestDTO;
import edu.dosw.restaurante.model.dto.response.MesaResponseDTO;
import edu.dosw.restaurante.service.IMesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mesas")
@RequiredArgsConstructor
public class MesaController implements MesaApi {

    private final IMesaService mesaService;
    private final MesaMapper mesaMapper;

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO')")
    public ResponseEntity<List<MesaResponseDTO>> obtenerTodas(@RequestParam(required = false) EstadoMesa estado) {
        List<Mesa> mesas = estado == null ? mesaService.obtenerTodas() : mesaService.obtenerPorEstado(estado);
        return ResponseEntity.ok(mesaMapper.toResponseList(mesas));
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO')")
    public ResponseEntity<MesaResponseDTO> obtenerPorId(@PathVariable Long id) {
        Mesa mesa = mesaService.obtenerPorId(id);
        return ResponseEntity.ok(mesaMapper.toResponse(mesa));
    }

    @Override
    @PostMapping
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<MesaResponseDTO> crear(@Valid @RequestBody MesaRequestDTO request) {
        Mesa mesa = mesaMapper.toDomain(request);
        Mesa creada = mesaService.crear(mesa);
        return ResponseEntity.status(HttpStatus.CREATED).body(mesaMapper.toResponse(creada));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<MesaResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody MesaRequestDTO request) {
        Mesa actualizada = mesaService.actualizar(id, mesaMapper.toDomain(request));
        return ResponseEntity.ok(mesaMapper.toResponse(actualizada));
    }

    @Override
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO')")
    public ResponseEntity<MesaResponseDTO> cambiarEstado(@PathVariable Long id, @RequestParam EstadoMesa estado) {
        Mesa mesa = mesaService.cambiarEstado(id, estado);
        return ResponseEntity.ok(mesaMapper.toResponse(mesa));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mesaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
