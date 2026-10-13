package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.controller.docs.CuentaApi;
import edu.dosw.restaurante.mapper.CuentaMapper;
import edu.dosw.restaurante.model.domain.Cuenta;
import edu.dosw.restaurante.model.domain.EstadoCuenta;
import edu.dosw.restaurante.model.dto.request.AbrirCuentaRequestDTO;
import edu.dosw.restaurante.model.dto.request.PagoRequestDTO;
import edu.dosw.restaurante.model.dto.response.CuentaResponseDTO;
import edu.dosw.restaurante.service.ICuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cuentas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('GERENTE', 'MESERO')") // aplica a todos los métodos de la clase
public class CuentaController implements CuentaApi {

    private final ICuentaService cuentaService;
    private final CuentaMapper cuentaMapper;

    @Override
    @GetMapping
    public ResponseEntity<List<CuentaResponseDTO>> obtenerTodas(@RequestParam(required = false) EstadoCuenta estado) {
        List<Cuenta> cuentas = estado == null ? cuentaService.obtenerTodas() : cuentaService.obtenerPorEstado(estado);
        return ResponseEntity.ok(cuentaMapper.toResponseList(cuentas));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<CuentaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaMapper.toResponse(cuentaService.obtenerPorId(id)));
    }

    @Override
    @GetMapping("/mesa/{idMesa}")
    public ResponseEntity<CuentaResponseDTO> obtenerAbiertaPorMesa(@PathVariable Long idMesa) {
        return ResponseEntity.ok(cuentaMapper.toResponse(cuentaService.obtenerAbiertaPorMesa(idMesa)));
    }

    @Override
    @PostMapping
    public ResponseEntity<CuentaResponseDTO> abrir(@Valid @RequestBody AbrirCuentaRequestDTO request) {
        Cuenta abierta = cuentaService.abrir(request.getIdMesa());
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaMapper.toResponse(abierta));
    }

    @Override
    @PatchMapping("/{id}/solicitar")
    public ResponseEntity<CuentaResponseDTO> solicitarPago(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaMapper.toResponse(cuentaService.solicitarPago(id)));
    }

    @Override
    @PostMapping("/{id}/pago")
    public ResponseEntity<CuentaResponseDTO> pagar(@PathVariable Long id, @Valid @RequestBody PagoRequestDTO request) {
        Cuenta pagada = cuentaService.pagar(id, request.getMetodoPago(), request.getMontoRecibido());
        return ResponseEntity.ok(cuentaMapper.toResponse(pagada));
    }
}
