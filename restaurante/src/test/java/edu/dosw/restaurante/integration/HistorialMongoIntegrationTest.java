package edu.dosw.restaurante.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.restaurante.repository.CuentaRepository;
import edu.dosw.restaurante.repository.EventoPedidoRepository;
import edu.dosw.restaurante.repository.MesaRepository;
import edu.dosw.restaurante.repository.PedidoRepository;
import edu.dosw.restaurante.repository.PlatoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Las dos bases juntas: el pedido va a H2 (relacional) y su historial a MongoDB (no relacional).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@WithMockUser(roles = "GERENTE")
class HistorialMongoIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventoPedidoRepository eventoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PlatoRepository platoRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @BeforeEach
    void limpiar() {
        eventoRepository.deleteAll();
        pedidoRepository.deleteAll();
        cuentaRepository.deleteAll();
        platoRepository.deleteAll();
        mesaRepository.deleteAll();
    }

    private long idDe(String url, String body) throws Exception {
        String json = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    // registrar() es @Async: el evento llega a Mongo unos milisegundos después de la respuesta HTTP
    private JsonNode esperarHistorial(long idPedido, int eventosEsperados) throws Exception {
        JsonNode historial = null;
        for (int intento = 0; intento < 50; intento++) {
            String json = mockMvc.perform(get("/api/v1/pedidos/" + idPedido + "/historial"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            historial = objectMapper.readTree(json);
            if (historial.size() >= eventosEsperados) {
                return historial;
            }
            Thread.sleep(100);
        }
        fail("Se esperaban " + eventosEsperados + " eventos y hay " + historial.size() + ": " + historial);
        return historial;
    }

    @Test
    void cicloDeVidaDelPedido_QuedaEnElHistorial() throws Exception {
        long idPlato = idDe("/api/v1/platos",
                "{\"nombre\":\"Dragon Roll\",\"precio\":22000,\"categoria\":\"Rolls\",\"disponible\":true}");
        long idMesa = idDe("/api/v1/mesas", "{\"numero\":1,\"capacidad\":4}");
        idDe("/api/v1/cuentas", "{\"idMesa\":" + idMesa + "}");
        String pedidoJson = "{\"idMesa\":" + idMesa + ",\"items\":[{\"idPlato\":" + idPlato + ",\"cantidad\":2}]}";
        long idPedido = idDe("/api/v1/pedidos", pedidoJson);

        esperarHistorial(idPedido, 1); // asegura el orden de los eventos
        mockMvc.perform(put("/api/v1/pedidos/" + idPedido).contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson.replace("\"cantidad\":2", "\"cantidad\":3")));
        esperarHistorial(idPedido, 2);
        mockMvc.perform(patch("/api/v1/pedidos/" + idPedido + "/estado").param("estado", "CANCELADO"));

        JsonNode historial = esperarHistorial(idPedido, 3);

        assertEquals("CREADO", historial.get(0).get("tipo").asText());
        assertEquals(44000.0, historial.get(0).get("detalle").get("total").asDouble());
        assertEquals("Dragon Roll", historial.get(0).get("detalle").get("items").get(0).get("nombrePlato").asText());

        assertEquals("MODIFICADO", historial.get(1).get("tipo").asText());
        assertEquals(44000.0, historial.get(1).get("detalle").get("totalAnterior").asDouble());
        assertEquals(66000.0, historial.get(1).get("detalle").get("total").asDouble());

        assertEquals("CAMBIO_ESTADO", historial.get(2).get("tipo").asText());
        assertEquals("RECIBIDO", historial.get(2).get("estadoAnterior").asText());
        assertEquals("CANCELADO", historial.get(2).get("estadoNuevo").asText());
    }

    @Test
    void pedidoEliminado_ElHistorialSeConserva() throws Exception {
        long idPlato = idDe("/api/v1/platos",
                "{\"nombre\":\"Nigiri\",\"precio\":12000,\"categoria\":\"Sushi\",\"disponible\":true}");
        long idMesa = idDe("/api/v1/mesas", "{\"numero\":2,\"capacidad\":2}");
        idDe("/api/v1/cuentas", "{\"idMesa\":" + idMesa + "}");
        long idPedido = idDe("/api/v1/pedidos",
                "{\"idMesa\":" + idMesa + ",\"items\":[{\"idPlato\":" + idPlato + ",\"cantidad\":1}]}");
        esperarHistorial(idPedido, 1);

        mockMvc.perform(delete("/api/v1/pedidos/" + idPedido)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/pedidos/" + idPedido)).andExpect(status().isNotFound()); // ya no está en H2
        JsonNode historial = esperarHistorial(idPedido, 2);                                   // pero sí en Mongo
        assertEquals("ELIMINADO", historial.get(1).get("tipo").asText());
        assertTrue(historial.get(1).get("estadoNuevo").isNull());
    }

    @Test
    void operacionRechazada_NoGeneraEvento() throws Exception {
        long idMesa = idDe("/api/v1/mesas", "{\"numero\":3,\"capacidad\":2}");

        // Mesa sin cuenta abierta: 422, el pedido no se crea y no hay evento
        mockMvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMesa\":" + idMesa + ",\"items\":[{\"idPlato\":1,\"cantidad\":1}]}"))
                .andExpect(status().isUnprocessableEntity());

        Thread.sleep(300);
        assertEquals(0, eventoRepository.count());
    }
}
