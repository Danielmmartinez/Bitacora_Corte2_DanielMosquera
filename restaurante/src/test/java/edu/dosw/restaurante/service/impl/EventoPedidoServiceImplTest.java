package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.mapper.EventoPedidoDocumentMapper;
import edu.dosw.restaurante.mapper.EventoPedidoDocumentMapperImpl;
import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.domain.EventoPedido;
import edu.dosw.restaurante.model.domain.TipoEventoPedido;
import edu.dosw.restaurante.persistence.document.EventoPedidoDocument;
import edu.dosw.restaurante.repository.EventoPedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Prueba unitaria (sin Mongo). Aquí @Async no aplica porque no hay contexto de Spring:
 * registrar() se ejecuta en el mismo hilo, lo que permite verificarlo directamente.
 */
@ExtendWith(MockitoExtension.class)
class EventoPedidoServiceImplTest {

    @Mock
    private EventoPedidoRepository eventoRepository;

    @Spy
    private EventoPedidoDocumentMapper documentMapper = new EventoPedidoDocumentMapperImpl();

    @InjectMocks
    private EventoPedidoServiceImpl eventoService;

    @Test
    void registrar_GuardaDocumentoSinIdYConFecha() {
        EventoPedido evento = EventoPedido.builder().id("no-debe-usarse").idPedido(1L)
                .tipo(TipoEventoPedido.CREADO).estadoNuevo(EstadoPedido.RECIBIDO)
                .detalle(Map.of("total", 40000.0)).build();

        eventoService.registrar(evento);

        ArgumentCaptor<EventoPedidoDocument> captor = ArgumentCaptor.forClass(EventoPedidoDocument.class);
        verify(eventoRepository).save(captor.capture());
        EventoPedidoDocument guardado = captor.getValue();
        assertNull(guardado.getId()); // Mongo genera el ObjectId
        assertNotNull(guardado.getFecha());
        assertEquals(TipoEventoPedido.CREADO, guardado.getTipo());
        assertEquals(40000.0, guardado.getDetalle().get("total"));
    }

    @Test
    void registrar_MongoCaido_NoPropagaLaExcepcion() {
        when(eventoRepository.save(any())).thenThrow(new DataAccessResourceFailureException("Mongo apagado"));
        EventoPedido evento = EventoPedido.builder().idPedido(1L).tipo(TipoEventoPedido.CREADO).build();

        assertDoesNotThrow(() -> eventoService.registrar(evento));
    }

    @Test
    void obtenerHistorial_ConvierteDocumentosADominio() {
        LocalDateTime ahora = LocalDateTime.now();
        when(eventoRepository.findByIdPedidoOrderByFechaAsc(1L)).thenReturn(List.of(
                EventoPedidoDocument.builder().id("a").idPedido(1L).tipo(TipoEventoPedido.CREADO).fecha(ahora).build(),
                EventoPedidoDocument.builder().id("b").idPedido(1L).tipo(TipoEventoPedido.CAMBIO_ESTADO)
                        .estadoAnterior(EstadoPedido.RECIBIDO).estadoNuevo(EstadoPedido.EN_PREPARACION)
                        .fecha(ahora.plusMinutes(1)).build()));

        List<EventoPedido> historial = eventoService.obtenerHistorial(1L);

        assertEquals(2, historial.size());
        assertEquals("a", historial.get(0).getId());
        assertEquals(EstadoPedido.EN_PREPARACION, historial.get(1).getEstadoNuevo());
    }

    @Test
    void obtenerHistorial_SinEventos_DevuelveListaVacia() {
        when(eventoRepository.findByIdPedidoOrderByFechaAsc(9L)).thenReturn(List.of());
        assertTrue(eventoService.obtenerHistorial(9L).isEmpty());
    }

    @Test
    void mapper_NulosDevuelvenNulo() {
        assertNull(documentMapper.toDocument(null));
        assertNull(documentMapper.toDomain(null));
    }
}
