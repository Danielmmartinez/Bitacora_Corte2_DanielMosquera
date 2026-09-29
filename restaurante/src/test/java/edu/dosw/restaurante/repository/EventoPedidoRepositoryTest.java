package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.domain.TipoEventoPedido;
import edu.dosw.restaurante.persistence.document.EventoPedidoDocument;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @DataMongoTest levanta solo la capa de Spring Data MongoDB.
 * Testcontainers arranca un MongoDB real en Docker; @ServiceConnection conecta Spring a él.
 * Sin Docker, estas pruebas se saltan (no fallan).
 */
@DataMongoTest
@Testcontainers(disabledWithoutDocker = true)
class EventoPedidoRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired
    private EventoPedidoRepository eventoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void limpiar() {
        eventoRepository.deleteAll();
    }

    private EventoPedidoDocument evento(long idPedido, TipoEventoPedido tipo, LocalDateTime fecha, Map<String, Object> detalle) {
        return EventoPedidoDocument.builder().idPedido(idPedido).idMesa(1L).tipo(tipo)
                .estadoNuevo(EstadoPedido.RECIBIDO).fecha(fecha).detalle(detalle).build();
    }

    @Test
    void save_MongoGeneraElId() {
        EventoPedidoDocument guardado = eventoRepository.save(
                evento(1, TipoEventoPedido.CREADO, LocalDateTime.now(), Map.of()));

        assertNotNull(guardado.getId());
        assertEquals(24, guardado.getId().length()); // ObjectId en hexadecimal
    }

    @Test
    void findByIdPedidoOrderByFechaAsc_FiltraYOrdena() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);
        eventoRepository.save(evento(1, TipoEventoPedido.CAMBIO_ESTADO, base.plusMinutes(5), Map.of()));
        eventoRepository.save(evento(1, TipoEventoPedido.CREADO, base, Map.of()));
        eventoRepository.save(evento(2, TipoEventoPedido.CREADO, base, Map.of()));

        List<EventoPedidoDocument> historial = eventoRepository.findByIdPedidoOrderByFechaAsc(1L);

        assertEquals(2, historial.size());
        assertEquals(TipoEventoPedido.CREADO, historial.get(0).getTipo());
        assertEquals(TipoEventoPedido.CAMBIO_ESTADO, historial.get(1).getTipo());
    }

    @Test
    void detalle_SeGuardaComoSubdocumentoConFormaLibre() {
        eventoRepository.save(evento(1, TipoEventoPedido.CREADO, LocalDateTime.now(), Map.of(
                "total", 44000.0,
                "items", List.of(Map.of("nombrePlato", "Dragon Roll", "cantidad", 2)))));
        eventoRepository.save(evento(1, TipoEventoPedido.ELIMINADO, LocalDateTime.now(), Map.of("total", 44000.0)));

        // Lectura "cruda" de la colección: así se ve el documento dentro de MongoDB
        Document crudo = mongoTemplate.getCollection("eventos_pedido")
                .find(new Document("tipo", "CREADO")).first();

        assertNotNull(crudo);
        assertEquals("RECIBIDO", crudo.get("estadoNuevo")); // los enums se guardan como texto
        Document detalle = crudo.get("detalle", Document.class);
        List<?> items = detalle.get("items", List.class);
        assertEquals("Dragon Roll", ((Document) items.get(0)).get("nombrePlato"));
    }
}
