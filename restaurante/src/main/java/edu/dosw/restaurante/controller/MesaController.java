package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.model.dto.request.MesaRequestDTO;
import edu.dosw.restaurante.model.dto.response.MesaResponseDTO;
import edu.dosw.restaurante.mapper.MesaMapper;
import edu.dosw.restaurante.service.IMesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mesas")
@RequiredArgsConstructor
public class MesaController {

    private final IMesaService mesaService;
    private final MesaMapper mesaMapper;

    @GetMapping
    public ResponseEntity<List<MesaResponseDTO>> obtenerTodas() {
        List<Mesa> mesas = mesaService.obtenerTodas();
        return ResponseEntity.ok(mesaMapper.toResponseList(mesas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MesaResponseDTO> obtenerPorId(@PathVariable Long id) {
        Mesa mesa = mesaService.obtenerPorId(id);
        return ResponseEntity.ok(mesaMapper.toResponse(mesa));
    }

    @PostMapping
    public ResponseEntity<MesaResponseDTO> crear(@Valid @RequestBody MesaRequestDTO request) {
        Mesa mesa = mesaMapper.toDomain(request);
        Mesa creada = mesaService.crear(mesa);
        return ResponseEntity.status(HttpStatus.CREATED).body(mesaMapper.toResponse(creada));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<MesaResponseDTO> cambiarEstado(@PathVariable Long id, @RequestParam EstadoMesa estado) {
        Mesa mesa = mesaService.cambiarEstado(id, estado);
        return ResponseEntity.ok(mesaMapper.toResponse(mesa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mesaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}