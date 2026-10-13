package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.controller.docs.ReservaApi;
import edu.dosw.restaurante.mapper.MesaMapper;
import edu.dosw.restaurante.mapper.ReservaMapper;
import edu.dosw.restaurante.model.domain.Reserva;
import edu.dosw.restaurante.model.dto.request.ReservaRequestDTO;
import edu.dosw.restaurante.model.dto.response.MesaResponseDTO;
import edu.dosw.restaurante.model.dto.response.ReservaResponseDTO;
import edu.dosw.restaurante.service.IReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
public class ReservaController implements ReservaApi {

    private static final String PERSONAL_O_CLIENTE = "hasAnyRole('GERENTE', 'MESERO', 'CLIENTE')";

    private final IReservaService reservaService;
    private final ReservaMapper reservaMapper;
    private final MesaMapper mesaMapper;

    @Override
    @GetMapping
    @PreAuthorize(PERSONAL_O_CLIENTE)
    public ResponseEntity<List<ReservaResponseDTO>> obtener(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Authentication auth) {
        String cliente = propietario(auth);
        List<Reserva> reservas = cliente != null
                ? reservaService.obtenerDeCliente(cliente)
                : reservaService.obtenerTodas(fecha);
        return ResponseEntity.ok(reservaMapper.toResponseList(reservas));
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize(PERSONAL_O_CLIENTE)
    public ResponseEntity<ReservaResponseDTO> obtenerPorId(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(reservaMapper.toResponse(reservaService.obtenerPorId(id, propietario(auth))));
    }

    @Override
    @GetMapping("/disponibilidad")
    @PreAuthorize(PERSONAL_O_CLIENTE)
    public ResponseEntity<List<MesaResponseDTO>> disponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora,
            @RequestParam int comensales) {
        return ResponseEntity.ok(mesaMapper.toResponseList(reservaService.mesasDisponibles(fechaHora, comensales)));
    }

    @Override
    @PostMapping
    @PreAuthorize(PERSONAL_O_CLIENTE)
    public ResponseEntity<ReservaResponseDTO> crear(@Valid @RequestBody ReservaRequestDTO request, Authentication auth) {
        Reserva reserva = reservaMapper.toDomain(request);
        String cliente = propietario(auth);
        if (cliente != null) {
            reserva.setEmailCliente(cliente); // un cliente no puede reservar a nombre de otro
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaMapper.toResponse(reservaService.crear(reserva)));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize(PERSONAL_O_CLIENTE)
    public ResponseEntity<ReservaResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ReservaRequestDTO request,
                                                         Authentication auth) {
        Reserva actualizada = reservaService.actualizar(id, reservaMapper.toDomain(request), propietario(auth));
        return ResponseEntity.ok(reservaMapper.toResponse(actualizada));
    }

    @Override
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize(PERSONAL_O_CLIENTE)
    public ResponseEntity<ReservaResponseDTO> cancelar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(reservaMapper.toResponse(reservaService.cancelar(id, propietario(auth))));
    }

    @Override
    @PatchMapping("/{id}/llegada")
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO')")
    public ResponseEntity<ReservaResponseDTO> registrarLlegada(@PathVariable Long id) {
        return ResponseEntity.ok(reservaMapper.toResponse(reservaService.registrarLlegada(id)));
    }

    /** Email del token si quien llama es CLIENTE; null si es personal (sin restricción). */
    private String propietario(Authentication auth) {
        boolean esCliente = auth.getAuthorities().stream().anyMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority()));
        return esCliente ? auth.getName() : null;
    }
}
