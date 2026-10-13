package edu.dosw.restaurante.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.repository.*;
import edu.dosw.restaurante.service.IEventoPedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Un día completo del restaurante, con varios actores y un reloj fijo (15/06/2030 10:00):
 * reservas → llegada → pedido → pago → parqueadero → reportes del día.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OperacionIntegrationTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 6, 15, 10, 0);

    // Reemplaza el reloj real por uno fijo solo en esta prueba
    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        }
    }

    private static final RequestPostProcessor GERENTE = user("gerente@sakura.com").roles("GERENTE");
    private static final RequestPostProcessor MESERO = user("mesero@sakura.com").roles("MESERO");
    private static final RequestPostProcessor COCINERO = user("cocinero@sakura.com").roles("COCINERO");
    private static final RequestPostProcessor ANA = user("ana@mail.com").roles("CLIENTE");
    private static final RequestPostProcessor BETO = user("beto@mail.com").roles("CLIENTE");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private CuentaRepository cuentaRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private RegistroVehiculoRepository registroRepository;
    @Autowired private PlatoRepository platoRepository;
    @Autowired private MesaRepository mesaRepository;

    @MockBean
    private IEventoPedidoService eventoService; // el historial (Mongo) no es parte de esta prueba

    @BeforeEach
    void limpiar() {
        pedidoRepository.deleteAll();
        cuentaRepository.deleteAll();
        reservaRepository.deleteAll();
        registroRepository.deleteAll();
        platoRepository.deleteAll();
        mesaRepository.deleteAll();
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, RequestPostProcessor quien, String body) throws Exception {
        return mockMvc.perform(peticion.with(quien).contentType(MediaType.APPLICATION_JSON).content(body == null ? "" : body));
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private String reservaJson(long idMesa, String hora, int comensales) {
        return "{\"idMesa\":" + idMesa + ",\"nombreCliente\":\"Cliente\",\"fechaHora\":\"2030-06-15T" + hora
                + ":00\",\"comensales\":" + comensales + "}";
    }

    @Test
    void unDiaCompletoDelRestaurante() throws Exception {
        // ── El gerente prepara la carta y el salón
        long idRamen = json(enviar(post("/api/v1/platos"), GERENTE,
                "{\"nombre\":\"Ramen\",\"precio\":25000,\"categoria\":\"Sopas\",\"disponible\":true}")
                .andExpect(status().isCreated())).get("id").asLong();
        json(enviar(post("/api/v1/mesas"), GERENTE, "{\"numero\":1,\"capacidad\":2}").andExpect(status().isCreated()));
        long mesa2 = json(enviar(post("/api/v1/mesas"), GERENTE, "{\"numero\":2,\"capacidad\":4}")
                .andExpect(status().isCreated())).get("id").asLong();

        // ── Ana busca mesa para 3 a las 19:00: solo sirve la mesa 2
        enviar(get("/api/v1/reservas/disponibilidad").param("fechaHora", "2030-06-15T19:00:00").param("comensales", "3"), ANA, null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numero").value(2));

        long reservaAna = json(enviar(post("/api/v1/reservas"), ANA, reservaJson(mesa2, "19:00", 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.emailCliente").value("ana@mail.com"))).get("id").asLong();

        // ── Beto no puede reservar la misma mesa a las 20:00 (se cruza), ni ver la reserva de Ana
        enviar(post("/api/v1/reservas"), BETO, reservaJson(mesa2, "20:00", 2)).andExpect(status().isConflict());
        enviar(post("/api/v1/reservas"), BETO, reservaJson(mesa2, "21:00", 2)).andExpect(status().isCreated());
        enviar(get("/api/v1/reservas/" + reservaAna), BETO, null).andExpect(status().isNotFound());
        enviar(get("/api/v1/reservas"), BETO, null).andExpect(jsonPath("$.length()").value(1));
        enviar(get("/api/v1/reservas").param("fecha", "2030-06-15"), MESERO, null).andExpect(jsonPath("$.length()").value(2));

        // Ya no hay mesa para 3 a las 19:00
        enviar(get("/api/v1/reservas/disponibilidad").param("fechaHora", "2030-06-15T19:00:00").param("comensales", "3"), ANA, null)
                .andExpect(jsonPath("$.length()").value(0));

        // ── Ana llega: la reserva se cumple y se abre la cuenta de la mesa
        long idCuenta = json(enviar(patch("/api/v1/reservas/" + reservaAna + "/llegada"), MESERO, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CUMPLIDA"))).get("idCuenta").asLong();
        assertEquals(EstadoMesa.OCUPADA, mesaRepository.findById(mesa2).orElseThrow().getEstado());
        enviar(patch("/api/v1/reservas/" + reservaAna + "/cancelar"), ANA, null).andExpect(status().isUnprocessableEntity());

        // ── Pide, la cocina prepara, se entrega y se paga
        long idPedido = json(enviar(post("/api/v1/pedidos"), MESERO,
                "{\"idMesa\":" + mesa2 + ",\"items\":[{\"idPlato\":" + idRamen + ",\"cantidad\":2}]}")
                .andExpect(jsonPath("$.idCuenta").value(idCuenta))).get("id").asLong();
        enviar(patch("/api/v1/pedidos/" + idPedido + "/estado").param("estado", "EN_PREPARACION"), COCINERO, null).andExpect(status().isOk());
        enviar(patch("/api/v1/pedidos/" + idPedido + "/estado").param("estado", "LISTO"), COCINERO, null).andExpect(status().isOk());
        enviar(patch("/api/v1/pedidos/" + idPedido + "/estado").param("estado", "ENTREGADO"), MESERO, null).andExpect(status().isOk());
        enviar(post("/api/v1/cuentas/" + idCuenta + "/pago"), MESERO, "{\"metodoPago\":\"EFECTIVO\",\"montoRecibido\":50000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaCierre").value("2030-06-15T10:00:00"));

        // ── Parqueadero: entra, no puede entrar dos veces, sale y paga la hora mínima
        enviar(post("/api/v1/parqueadero/entrada"), MESERO, "{\"placa\":\"abc-123\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value("ABC123"));
        enviar(post("/api/v1/parqueadero/entrada"), MESERO, "{\"placa\":\"ABC 123\"}").andExpect(status().isConflict());
        enviar(get("/api/v1/parqueadero/estado"), MESERO, null).andExpect(jsonPath("$.ocupados").value(1));
        enviar(post("/api/v1/parqueadero/salida/abc123"), MESERO, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cobro").value(5000.0));

        // ── Reportes del día (solo el gerente)
        enviar(get("/api/v1/reportes/resumen"), MESERO, null).andExpect(status().isForbidden());
        enviar(get("/api/v1/reportes/resumen"), GERENTE, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value("2030-06-15"))
                .andExpect(jsonPath("$.totalPedidos").value(1))
                .andExpect(jsonPath("$.pedidosPorEstado.ENTREGADO").value(1))
                .andExpect(jsonPath("$.platoMasVendido").value("Ramen"))
                .andExpect(jsonPath("$.ingresosRestaurante").value(50000.0))
                .andExpect(jsonPath("$.reservas").value(2))
                .andExpect(jsonPath("$.ingresosParqueadero").value(5000.0))
                .andExpect(jsonPath("$.mesasOcupadasAhora").value(0));
        enviar(get("/api/v1/reportes/ingresos"), GERENTE, null)
                .andExpect(jsonPath("$.totalGeneral").value(55000.0))
                .andExpect(jsonPath("$.porMetodoPago.EFECTIVO").value(50000.0))
                .andExpect(jsonPath("$.porCategoria.Sopas").value(50000.0));
        enviar(get("/api/v1/reportes/platos-mas-vendidos"), GERENTE, null)
                .andExpect(jsonPath("$[0].nombrePlato").value("Ramen"))
                .andExpect(jsonPath("$[0].unidades").value(2));
    }

    @Test
    void reservaFueraDeHorarioOExcesoDeComensales() throws Exception {
        long mesa = json(enviar(post("/api/v1/mesas"), GERENTE, "{\"numero\":7,\"capacidad\":2}")).get("id").asLong();

        enviar(post("/api/v1/reservas"), ANA, reservaJson(mesa, "23:00", 2)).andExpect(status().isUnprocessableEntity());
        enviar(post("/api/v1/reservas"), ANA, reservaJson(mesa, "19:00", 5)).andExpect(status().isUnprocessableEntity());
        enviar(post("/api/v1/reservas"), ANA, reservaJson(9999, "19:00", 2)).andExpect(status().isNotFound());
        assertEquals(0, reservaRepository.count());
    }
}
