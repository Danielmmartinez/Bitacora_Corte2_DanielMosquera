package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.controller.docs.PedidoApi;
import edu.dosw.restaurante.mapper.EventoPedidoMapper;
import edu.dosw.restaurante.mapper.PedidoMapper;
import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.domain.Pedido;
import edu.dosw.restaurante.model.dto.request.PedidoRequestDTO;
import edu.dosw.restaurante.model.dto.response.EventoPedidoResponseDTO;
import edu.dosw.restaurante.model.dto.response.PedidoResponseDTO;
import edu.dosw.restaurante.service.IEventoPedidoService;
import edu.dosw.restaurante.service.IPedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController implements PedidoApi {

    private final IPedidoService pedidoService;
    private final PedidoMapper pedidoMapper;
    private final IEventoPedidoService eventoService;
    private final EventoPedidoMapper eventoMapper;

    @Override
    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO', 'COCINERO')")
    public ResponseEntity<List<PedidoResponseDTO>> obtenerTodos(@RequestParam(required = false) EstadoPedido estado) {
        List<Pedido> pedidos = estado == null ? pedidoService.obtenerTodos() : pedidoService.obtenerPorEstado(estado);
        return ResponseEntity.ok(pedidoMapper.toResponseList(pedidos));
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO', 'COCINERO')")
    public ResponseEntity<PedidoResponseDTO> obtenerPorId(@PathVariable Long id) {
        Pedido pedido = pedidoService.obtenerPorId(id);
        return ResponseEntity.ok(pedidoMapper.toResponse(pedido));
    }

    @Override
    @GetMapping("/mesa/{idMesa}")
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO', 'COCINERO')")
    public ResponseEntity<List<PedidoResponseDTO>> obtenerPorMesa(@PathVariable Long idMesa) {
        List<Pedido> pedidos = pedidoService.obtenerPorMesa(idMesa);
        return ResponseEntity.ok(pedidoMapper.toResponseList(pedidos));
    }

    @Override
    @GetMapping("/{id}/historial")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<List<EventoPedidoResponseDTO>> obtenerHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(eventoMapper.toResponseList(eventoService.obtenerHistorial(id)));
    }

    @Override
    @PostMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO')")
    public ResponseEntity<PedidoResponseDTO> crear(@Valid @RequestBody PedidoRequestDTO request) {
        Pedido pedido = pedidoMapper.toDomain(request);
        Pedido creado = pedidoService.crear(pedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoMapper.toResponse(creado));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO')")
    public ResponseEntity<PedidoResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody PedidoRequestDTO request) {
        Pedido actualizado = pedidoService.actualizar(id, pedidoMapper.toDomain(request));
        return ResponseEntity.ok(pedidoMapper.toResponse(actualizado));
    }

    @Override
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO', 'COCINERO')")
    public ResponseEntity<PedidoResponseDTO> cambiarEstado(@PathVariable Long id, @RequestParam EstadoPedido estado) {
        Pedido pedido = pedidoService.cambiarEstado(id, estado);
        return ResponseEntity.ok(pedidoMapper.toResponse(pedido));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'MESERO')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pedidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
