package edu.dosw.restaurante.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.repository.CuentaRepository;
import edu.dosw.restaurante.repository.MesaRepository;
import edu.dosw.restaurante.repository.PedidoRepository;
import edu.dosw.restaurante.repository.PlatoRepository;
import edu.dosw.restaurante.service.IEventoPedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de punta a punta: HTTP → Controller → Service → Repository → H2 (en memoria).
 * No es @Transactional a propósito: cada petición usa su propia transacción, igual que en producción.
 * El historial (MongoDB) se reemplaza por un mock: se prueba con Mongo real en HistorialMongoIntegrationTest.
 * La seguridad se prueba en SeguridadIntegrationTest; aquí todas las peticiones van como gerente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "GERENTE")
class PersistenciaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatoRepository platoRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @MockBean
    private IEventoPedidoService eventoService;

    @BeforeEach
    void limpiarBD() {
        pedidoRepository.deleteAll();
        cuentaRepository.deleteAll();
        platoRepository.deleteAll();
        mesaRepository.deleteAll();
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private long crearPlato(String nombre, double precio) throws Exception {
        String body = "{\"nombre\":\"" + nombre + "\",\"precio\":" + precio + ",\"categoria\":\"Rolls\",\"disponible\":true}";
        return json(mockMvc.perform(post("/api/v1/platos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private long crearMesa(int numero) throws Exception {
        return json(mockMvc.perform(post("/api/v1/mesas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":" + numero + ",\"capacidad\":4}"))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private long abrirCuenta(long idMesa) throws Exception {
        return json(mockMvc.perform(post("/api/v1/cuentas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMesa\":" + idMesa + "}"))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private long crearMesaAbierta(int numero) throws Exception {
        long id = crearMesa(numero);
        abrirCuenta(id);
        return id;
    }

    private void pagar(long idCuenta, double monto, int statusEsperado) throws Exception {
        mockMvc.perform(post("/api/v1/cuentas/" + idCuenta + "/pago").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"EFECTIVO\",\"montoRecibido\":" + monto + "}"))
                .andExpect(status().is(statusEsperado));
    }

    private String pedidoJson(long idMesa, long idPlato, int cantidad) {
        return "{\"idMesa\":" + idMesa + ",\"items\":[{\"idPlato\":" + idPlato + ",\"cantidad\":" + cantidad + "}]}";
    }

    @Test
    void crearPlato_QuedaGuardadoEnLaBD() throws Exception {
        long id = crearPlato("Dragon Roll", 22000);

        assertTrue(platoRepository.findById(id).isPresent());
        mockMvc.perform(get("/api/v1/platos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Dragon Roll"));
    }

    @Test
    void platoDuplicado_Devuelve409() throws Exception {
        crearPlato("Dragon Roll", 22000);
        String body = "{\"nombre\":\"dragon roll\",\"precio\":1,\"categoria\":\"Rolls\",\"disponible\":true}";

        mockMvc.perform(post("/api/v1/platos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        assertEquals(1, platoRepository.count());
    }

    @Test
    void flujoCompletoDePedido() throws Exception {
        long idPlato = crearPlato("Dragon Roll", 22000);
        long idMesa = crearMesaAbierta(1);

        // Crear pedido: el pedido y su ítem se guardan juntos
        JsonNode pedido = json(mockMvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson(idMesa, idPlato, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(44000.0)));
        long idPedido = pedido.get("id").asLong();
        assertTrue(pedido.get("items").get(0).get("id").isNumber());

        // Cambiar el precio del plato NO altera el pedido (precio congelado en items_pedido)
        mockMvc.perform(put("/api/v1/platos/" + idPlato).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Dragon Roll\",\"precio\":30000,\"categoria\":\"Rolls\",\"disponible\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/pedidos/" + idPedido))
                .andExpect(jsonPath("$.items[0].precioCongelado").value(22000.0));

        // PUT del pedido: los ítems viejos se borran y queda solo el nuevo (con el precio actual)
        mockMvc.perform(put("/api/v1/pedidos/" + idPedido).contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson(idMesa, idPlato, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(30000.0));

        // Transiciones de estado persistidas
        mockMvc.perform(patch("/api/v1/pedidos/" + idPedido + "/estado").param("estado", "EN_PREPARACION"))
                .andExpect(status().isOk());
        assertEquals(EstadoPedido.EN_PREPARACION, pedidoRepository.findById(idPedido).orElseThrow().getEstado());

        mockMvc.perform(get("/api/v1/pedidos").param("estado", "EN_PREPARACION"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].items.length()").value(1));

        // Ya en cocina: no se puede borrar
        mockMvc.perform(delete("/api/v1/pedidos/" + idPedido))
                .andExpect(status().isUnprocessableEntity());
        assertTrue(pedidoRepository.existsById(idPedido));
    }

    @Test
    void eliminarPedidoRecibido_BorraPedidoEItems() throws Exception {
        long idPlato = crearPlato("Nigiri", 12000);
        long idMesa = crearMesaAbierta(2);
        long idPedido = json(mockMvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson(idMesa, idPlato, 1)))).get("id").asLong();

        mockMvc.perform(delete("/api/v1/pedidos/" + idPedido))
                .andExpect(status().isNoContent());

        assertFalse(pedidoRepository.existsById(idPedido));
        mockMvc.perform(get("/api/v1/pedidos/mesa/" + idMesa))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void mesa_SeOcupaConCuentaYSeLiberaAlPagar() throws Exception {
        long idMesa = crearMesa(3);
        long idCuenta = abrirCuenta(idMesa);

        var mesa = mesaRepository.findById(idMesa).orElseThrow();
        assertEquals(EstadoMesa.OCUPADA, mesa.getEstado());
        assertEquals(idCuenta, mesa.getIdCuentaAbierta());

        // Con la cuenta abierta: no se puede borrar, ni liberar a mano, ni abrir otra
        mockMvc.perform(delete("/api/v1/mesas/" + idMesa)).andExpect(status().isConflict());
        mockMvc.perform(patch("/api/v1/mesas/" + idMesa + "/estado").param("estado", "DISPONIBLE"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/api/v1/cuentas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idMesa\":" + idMesa + "}")).andExpect(status().isConflict());

        pagar(idCuenta, 0, 200); // se fueron sin pedir nada

        assertEquals(EstadoMesa.DISPONIBLE, mesaRepository.findById(idMesa).orElseThrow().getEstado());
        mockMvc.perform(delete("/api/v1/mesas/" + idMesa)).andExpect(status().isNoContent());
    }

    @Test
    void flujoCuentaYPago() throws Exception {
        long idPlato = crearPlato("Ramen", 25000);
        long idMesa = crearMesa(4);

        // Sin cuenta no hay pedidos
        mockMvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson(idMesa, idPlato, 1))).andExpect(status().isUnprocessableEntity());

        long idCuenta = abrirCuenta(idMesa);
        long p1 = json(mockMvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson(idMesa, idPlato, 2)))
                .andExpect(jsonPath("$.idCuenta").value(idCuenta))).get("id").asLong();
        long p2 = json(mockMvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content(pedidoJson(idMesa, idPlato, 1)))).get("id").asLong();

        // El segundo pedido se cancela: no se cobra
        mockMvc.perform(patch("/api/v1/pedidos/" + p2 + "/estado").param("estado", "CANCELADO"));
        mockMvc.perform(get("/api/v1/cuentas/mesa/" + idMesa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidos.length()").value(2))
                .andExpect(jsonPath("$.total").value(50000.0));

        // El cliente pide la cuenta
        mockMvc.perform(patch("/api/v1/cuentas/" + idCuenta + "/solicitar"))
                .andExpect(jsonPath("$.estado").value("EN_PAGO"));

        // No se puede pagar: el pedido 1 sigue en cocina
        pagar(idCuenta, 100000, 422);

        for (String estado : new String[]{"EN_PREPARACION", "LISTO", "ENTREGADO"}) {
            mockMvc.perform(patch("/api/v1/pedidos/" + p1 + "/estado").param("estado", estado))
                    .andExpect(status().isOk());
        }

        pagar(idCuenta, 40000, 422);     // monto insuficiente
        mockMvc.perform(post("/api/v1/cuentas/" + idCuenta + "/pago").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"EFECTIVO\",\"montoRecibido\":60000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.total").value(50000.0))
                .andExpect(jsonPath("$.cambio").value(10000.0));
        pagar(idCuenta, 60000, 422);     // ya pagada

        // Mesa libre; la cuenta cerrada conserva su total en la BD
        assertEquals(EstadoMesa.DISPONIBLE, mesaRepository.findById(idMesa).orElseThrow().getEstado());
        assertEquals(50000.0, cuentaRepository.findById(idCuenta).orElseThrow().getTotal());
        mockMvc.perform(get("/api/v1/cuentas/mesa/" + idMesa)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/cuentas").param("estado", "CERRADA"))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
