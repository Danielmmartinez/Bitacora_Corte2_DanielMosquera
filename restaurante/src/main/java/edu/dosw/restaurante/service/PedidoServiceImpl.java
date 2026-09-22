package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.domain.EstadoPedido;
import edu.dosw.restaurante.domain.ItemPedido;
import edu.dosw.restaurante.domain.Pedido;
import edu.dosw.restaurante.domain.Plato;
import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.service.IMesaService;
import edu.dosw.restaurante.service.IPedidoService;
import edu.dosw.restaurante.service.IPlatoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements IPedidoService {

    private final Map<Long, Pedido> repositorio = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);
    private final AtomicLong itemIdSequence = new AtomicLong(1);

    private final IPlatoService platoService;
    private final IMesaService mesaService;

    @Override
    public List<Pedido> obtenerTodos() {
        log.info("Consultando todos los pedidos en memoria");
        return repositorio.values().stream().toList();
    }

    @Override
    public Pedido obtenerPorId(Long id) {
        log.info("Buscando pedido con ID: {}", id);
        return repositorio.values().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el pedido con ID: " + id));
    }

    @Override
    public List<Pedido> obtenerPorMesa(Long idMesa) {
        log.info("Consultando pedidos para la mesa ID: {}", idMesa);
        mesaService.obtenerPorId(idMesa); // Valida que la mesa exista
        return repositorio.values().stream()
                .filter(p -> p.getIdMesa().equals(idMesa))
                .toList();
    }

    @Override
    public Pedido crear(Pedido pedido) {
        log.info("Creando nuevo pedido para la mesa ID: {}", pedido.getIdMesa());

        // 1. Validar que la mesa exista
        mesaService.obtenerPorId(pedido.getIdMesa());

        // 2. Validar cada ítem del pedido, congelar precio y enriquecer datos
        if (pedido.getItems() == null || pedido.getItems().isEmpty()) {
            throw new EstadoInvalidoException("Un pedido debe contener al menos un ítem");
        }

        for (ItemPedido item : pedido.getItems()) {
            Plato plato = platoService.obtenerPorId(item.getIdPlato());

            if (Boolean.FALSE.equals(plato.getDisponible())) {
                throw new EstadoInvalidoException("El plato '" + plato.getNombre() + "' no se encuentra disponible actualmente");
            }

            item.setId(itemIdSequence.getAndIncrement());
            item.setNombrePlato(plato.getNombre());
            item.setPrecioCongelado(plato.getPrecio());
        }

        // 3. Asignar metadatos al pedido
        Long nuevoId = idSequence.getAndIncrement();
        pedido.setId(nuevoId);
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setTimestamp(LocalDateTime.now());

        repositorio.put(nuevoId, pedido);
        return pedido;
    }

    @Override
    public Pedido cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        log.info("Cambiando estado del pedido ID: {} a {}", id, nuevoEstado);
        Pedido pedido = obtenerPorId(id);

        pedido.cambiarEstado(nuevoEstado);
        repositorio.put(id, pedido);

        return pedido;
    }

    @Override
    public void eliminar(Long id) {
        log.info("Eliminando pedido con ID: {}", id);
        obtenerPorId(id); // Valida existencia
        repositorio.remove(id);
    }
}