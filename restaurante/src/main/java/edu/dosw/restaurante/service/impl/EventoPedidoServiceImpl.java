package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.mapper.EventoPedidoDocumentMapper;
import edu.dosw.restaurante.model.domain.EventoPedido;
import edu.dosw.restaurante.repository.EventoPedidoRepository;
import edu.dosw.restaurante.service.IEventoPedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventoPedidoServiceImpl implements IEventoPedidoService {

    private final EventoPedidoRepository eventoRepository;
    private final EventoPedidoDocumentMapper documentMapper;

    /**
     * Se ejecuta en otro hilo (@Async): quien registra el evento no espera a MongoDB.
     * El historial es secundario: si Mongo no está disponible, el pedido igual se guarda
     * en H2 y aquí solo se deja constancia en el log.
     */
    @Override
    @Async
    public void registrar(EventoPedido evento) {
        try {
            evento.setId(null);
            if (evento.getFecha() == null) {
                evento.setFecha(LocalDateTime.now());
            }
            eventoRepository.save(documentMapper.toDocument(evento));
            log.info("Evento {} registrado para el pedido {}", evento.getTipo(), evento.getIdPedido());
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar el evento {} del pedido {} en MongoDB: {}",
                    evento.getTipo(), evento.getIdPedido(), ex.getMessage());
        }
    }

    @Override
    public List<EventoPedido> obtenerHistorial(Long idPedido) {
        log.info("Consultando historial del pedido {}", idPedido);
        return eventoRepository.findByIdPedidoOrderByFechaAsc(idPedido).stream()
                .map(documentMapper::toDomain)
                .toList();
    }
}
