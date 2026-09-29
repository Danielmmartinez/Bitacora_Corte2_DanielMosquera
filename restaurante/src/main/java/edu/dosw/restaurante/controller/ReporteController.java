package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.controller.docs.ReporteApi;
import edu.dosw.restaurante.mapper.ReporteMapper;
import edu.dosw.restaurante.model.dto.response.PlatoVendidoResponseDTO;
import edu.dosw.restaurante.model.dto.response.ReporteIngresosResponseDTO;
import edu.dosw.restaurante.model.dto.response.ResumenDiaResponseDTO;
import edu.dosw.restaurante.service.IReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('GERENTE')")
public class ReporteController implements ReporteApi {

    private final IReporteService reporteService;
    private final ReporteMapper reporteMapper;

    @Override
    @GetMapping("/resumen")
    public ResponseEntity<ResumenDiaResponseDTO> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(reporteMapper.toResponse(reporteService.resumenDelDia(fecha)));
    }

    @Override
    @GetMapping("/platos-mas-vendidos")
    public ResponseEntity<List<PlatoVendidoResponseDTO>> platosMasVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "5") int limite) {
        return ResponseEntity.ok(reporteMapper.toPlatosResponse(reporteService.platosMasVendidos(desde, hasta, limite)));
    }

    @Override
    @GetMapping("/ingresos")
    public ResponseEntity<ReporteIngresosResponseDTO> ingresos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteMapper.toResponse(reporteService.ingresos(desde, hasta)));
    }
}
