package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.controller.docs.ParqueaderoApi;
import edu.dosw.restaurante.mapper.ParqueaderoMapper;
import edu.dosw.restaurante.model.dto.request.EntradaVehiculoRequestDTO;
import edu.dosw.restaurante.model.dto.response.EstadoParqueaderoResponseDTO;
import edu.dosw.restaurante.model.dto.response.RegistroVehiculoResponseDTO;
import edu.dosw.restaurante.service.IParqueaderoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/parqueadero")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('GERENTE', 'MESERO')")
public class ParqueaderoController implements ParqueaderoApi {

    private final IParqueaderoService parqueaderoService;
    private final ParqueaderoMapper parqueaderoMapper;

    @Override
    @PostMapping("/entrada")
    public ResponseEntity<RegistroVehiculoResponseDTO> registrarEntrada(@Valid @RequestBody EntradaVehiculoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parqueaderoMapper.toResponse(parqueaderoService.registrarEntrada(request.getPlaca())));
    }

    @Override
    @PostMapping("/salida/{placa}")
    public ResponseEntity<RegistroVehiculoResponseDTO> registrarSalida(@PathVariable String placa) {
        return ResponseEntity.ok(parqueaderoMapper.toResponse(parqueaderoService.registrarSalida(placa)));
    }

    @Override
    @GetMapping("/estado")
    public ResponseEntity<EstadoParqueaderoResponseDTO> obtenerEstado() {
        return ResponseEntity.ok(parqueaderoMapper.toResponse(parqueaderoService.obtenerEstado()));
    }

    @Override
    @GetMapping("/registros")
    public ResponseEntity<List<RegistroVehiculoResponseDTO>> obtenerRegistros(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(parqueaderoMapper.toResponseList(parqueaderoService.obtenerRegistrosDelDia(fecha)));
    }

    @Override
    @GetMapping("/registros/{id}")
    public ResponseEntity<RegistroVehiculoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(parqueaderoMapper.toResponse(parqueaderoService.obtenerPorId(id)));
    }
}
