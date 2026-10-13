package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.mapper.PedidoEntityMapper;
import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.domain.EventoPedido;
import edu.dosw.restaurante.model.domain.ItemPedido;
import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.model.domain.Pedido;
import edu.dosw.restaurante.model.domain.Plato;
import edu.dosw.restaurante.model.domain.TipoEventoPedido;
import edu.dosw.restaurante.repository.PedidoRepository;
import edu.dosw.restaurante.service.IEventoPedidoService;
import edu.dosw.restaurante.service.IMesaService;
import edu.dosw.restaurante.service.IPedidoService;
import edu.dosw.restaurante.service.IPlatoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoServiceImpl implements IPedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoEntityMapper entityMapper;

    // Otros dominios se consultan a través de su Service (interfaz), nunca de su Repository
    private final IPlatoService platoService;
    private final IMesaService mesaService;

    // Historial en MongoDB (no relacional). Los pedidos en sí siguen en H2.
    private final IEventoPedidoService eventoService;
    private final Clock clock;

    @Override
    public List<Pedido> obtenerTodos() {
        log.info("Consultando todos los pedidos");
        return pedidoRepository.findAll().stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Pedido> obtenerPorEstado(EstadoPedido estado) {
        log.info("Consultando pedidos en estado: {}", estado);
        return pedidoRepository.findByEstado(estado).stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public Pedido obtenerPorId(Long id) {
        log.info("Buscando pedido con ID: {}", id);
        return pedidoRepository.findById(id)
                .map(entityMapper::toDomain)
                .orElseThrow(() -> {
                    log.warn("Pedido no encontrado: id={}", id);
                    return new RecursoNoEncontradoException("No se encontró el pedido con ID: " + id);
                });
    }

    @Override
    public List<Pedido> obtenerPorMesa(Long idMesa) {
        log.info("Consultando pedidos para la mesa ID: {}", idMesa);
        mesaService.obtenerPorId(idMesa); // Valida que la mesa exista
        return pedidoRepository.findByIdMesa(idMesa).stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Pedido> obtenerPorCuenta(Long idCuenta) {
        return pedidoRepository.findByIdCuenta(idCuenta).stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Pedido> obtenerCreadosEntre(LocalDateTime desde, LocalDateTime hasta) {
        return pedidoRepository.findCreadosEntre(desde, hasta).stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Pedido crear(Pedido pedido) {
        log.info("Creando nuevo pedido para la mesa ID: {}", pedido.getIdMesa());

        Mesa mesa = validarMesaConCuentaAbierta(pedido.getIdMesa());
        prepararItems(pedido.getItems());

        pedido.setId(null);
        pedido.setIdCuenta(mesa.getIdCuentaAbierta()); // el pedido se cobra en la cuenta abierta de la mesa
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setTimestamp(LocalDateTime.now(clock));

        // Un solo save guarda el pedido y, por cascada, todos sus ítems
        Pedido creado = guardar(pedido);
        log.info("Pedido #{} creado para la mesa ID: {}", creado.getId(), creado.getIdMesa());

        registrarEvento(TipoEventoPedido.CREADO, creado, null, creado.getEstado(), detalleConItems(creado));
        return creado;
    }

    @Override
    @Transactional
    public Pedido actualizar(Long id, Pedido pedidoActualizado) {
        log.info("Actualizando pedido con ID: {}", id);
        Pedido existente = obtenerPorId(id);

        if (!Boolean.TRUE.equals(existente.puedeModificarse())) {
            throw new EstadoInvalidoException(
                    "El pedido " + id + " está en estado " + existente.getEstado() + " y ya no puede modificarse");
        }

        Mesa mesa = validarMesaConCuentaAbierta(pedidoActualizado.getIdMesa());
        prepararItems(pedidoActualizado.getItems());
        Double totalAnterior = existente.calcularTotal();

        // Los ítems viejos quedan huérfanos y orphanRemoval los borra; los nuevos se insertan
        existente.setIdMesa(pedidoActualizado.getIdMesa());
        existente.setIdCuenta(mesa.getIdCuentaAbierta());
        existente.setItems(pedidoActualizado.getItems());
        Pedido actualizado = guardar(existente);

        Map<String, Object> detalle = detalleConItems(actualizado);
        detalle.put("totalAnterior", totalAnterior);
        registrarEvento(TipoEventoPedido.MODIFICADO, actualizado, actualizado.getEstado(), actualizado.getEstado(), detalle);
        return actualizado;
    }

    @Override
    @Transactional
    public Pedido cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        log.info("Cambiando estado del pedido ID: {} a {}", id, nuevoEstado);
        Pedido pedido = obtenerPorId(id);

        if (!pedido.puedeCambiarA(nuevoEstado)) {
            log.warn("Transición inválida del pedido {}: {} -> {}", id, pedido.getEstado(), nuevoEstado);
            throw new EstadoInvalidoException(
                    "El pedido no puede pasar de " + pedido.getEstado() + " a " + nuevoEstado);
        }

        EstadoPedido estadoAnterior = pedido.getEstado();
        pedido.cambiarEstado(nuevoEstado);
        Pedido actualizado = guardar(pedido);

        registrarEvento(TipoEventoPedido.CAMBIO_ESTADO, actualizado, estadoAnterior, nuevoEstado, new LinkedHashMap<>());
        return actualizado;
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando pedido con ID: {}", id);
        Pedido pedido = obtenerPorId(id);
        if (!Boolean.TRUE.equals(pedido.puedeModificarse())) {
            throw new EstadoInvalidoException(
                    "El pedido " + id + " está en estado " + pedido.getEstado() + " y ya no puede eliminarse");
        }
        pedidoRepository.deleteById(id); // cascade borra también sus ítems

        // El pedido desaparece de H2, pero su historial en Mongo se conserva
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("total", pedido.calcularTotal());
        registrarEvento(TipoEventoPedido.ELIMINADO, pedido, pedido.getEstado(), null, detalle);
    }

    private void registrarEvento(TipoEventoPedido tipo, Pedido pedido, EstadoPedido estadoAnterior,
                                 EstadoPedido estadoNuevo, Map<String, Object> detalle) {
        eventoService.registrar(EventoPedido.builder()
                .idPedido(pedido.getId())
                .idMesa(pedido.getIdMesa())
                .tipo(tipo)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .fecha(LocalDateTime.now(clock))
                .detalle(detalle)
                .build());
    }

    private Map<String, Object> detalleConItems(Pedido pedido) {
        List<Map<String, Object>> items = pedido.getItems().stream()
                .map(item -> {
                    Map<String, Object> resumen = new LinkedHashMap<>();
                    resumen.put("nombrePlato", item.getNombrePlato());
                    resumen.put("cantidad", item.getCantidad());
                    resumen.put("precioCongelado", item.getPrecioCongelado());
                    return resumen;
                })
                .toList();

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("total", pedido.calcularTotal());
        detalle.put("items", items);
        return detalle;
    }

    private Pedido guardar(Pedido pedido) {
        return entityMapper.toDomain(pedidoRepository.save(entityMapper.toEntity(pedido)));
    }

    private Mesa validarMesaConCuentaAbierta(Long idMesa) {
        Mesa mesa = mesaService.obtenerPorId(idMesa);
        if (!mesa.tieneCuentaAbierta()) {
            throw new EstadoInvalidoException("La mesa " + mesa.getNumero()
                    + " no tiene una cuenta abierta; ábrala con POST /api/v1/cuentas antes de registrar pedidos");
        }
        return mesa;
    }

    // Valida cada ítem, congela el precio del plato y completa sus datos
    private void prepararItems(List<ItemPedido> items) {
        if (items == null || items.isEmpty()) {
            throw new EstadoInvalidoException("Un pedido debe contener al menos un ítem");
        }

        for (ItemPedido item : items) {
            Plato plato = platoService.obtenerPorId(item.getIdPlato());

            if (!plato.estaDisponible()) {
                throw new EstadoInvalidoException("El plato '" + plato.getNombre() + "' no se encuentra disponible actualmente");
            }

            item.setId(null); // la BD asigna el ID del ítem
            item.setNombrePlato(plato.getNombre());
            item.setPrecioCongelado(plato.getPrecio());
        }
    }
}
