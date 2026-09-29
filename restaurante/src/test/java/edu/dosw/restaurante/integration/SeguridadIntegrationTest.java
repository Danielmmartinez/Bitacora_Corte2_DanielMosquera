package edu.dosw.restaurante.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.restaurante.repository.UsuarioRepository;
import edu.dosw.restaurante.service.IEventoPedidoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Seguridad de punta a punta, sin simulaciones: login real contra la BD, JWT real en el header
 * y las reglas de SecurityConfig + @PreAuthorize. Usa los usuarios demo (restaurante.seguridad.usuarios-demo).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean
    private IEventoPedidoService eventoService; // el historial (Mongo) no es parte de esta prueba

    private String token(String email, String password) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    private String tokenDe(String rol) throws Exception {
        return switch (rol) {
            case "GERENTE" -> token("gerente@sakura.com", "Gerente123!");
            case "MESERO" -> token("mesero@sakura.com", "Mesero123!");
            case "COCINERO" -> token("cocinero@sakura.com", "Cocinero123!");
            case "CLIENTE" -> token("cliente@sakura.com", "Cliente123!");
            default -> throw new IllegalArgumentException(rol);
        };
    }

    @Test
    void usuariosDemo_GuardanLaContrasenaComoHashBCrypt() {
        String hash = usuarioRepository.findByEmailIgnoreCase("gerente@sakura.com").orElseThrow().getPassword();
        assertTrue(hash.startsWith("$2a$"));
        assertNotEquals("Gerente123!", hash);
    }

    @Test
    void login_Correcto_DevuelveTokenYDatosDelUsuario() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mesero@sakura.com\",\"password\":\"Mesero123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(3600))
                .andExpect(jsonPath("$.usuario.rol").value("MESERO"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist());
    }

    @Test
    void login_PasswordIncorrecta_Devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mesero@sakura.com\",\"password\":\"otra\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_EmailInexistente_Devuelve401ConElMismoMensaje() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@sakura.com\",\"password\":\"Algo1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void yo_ConToken_DevuelveElUsuarioDelToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/yo").header("Authorization", "Bearer " + tokenDe("COCINERO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cocinero@sakura.com"))
                .andExpect(jsonPath("$.rol").value("COCINERO"));
    }

    @Test
    void sinToken_Devuelve401_TokenInventado_Devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/yo")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/platos").header("Authorization", "Bearer inventado.no.valido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void menu_EsPublico() throws Exception {
        mockMvc.perform(get("/api/v1/menu")).andExpect(status().isOk());
    }

    @Test
    void swagger_EsPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void registro_CreaClienteQuePuedeIniciarSesion() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"ana.registro@mail.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        String tokenAna = token("ana.registro@mail.com", "Secreta123");
        mockMvc.perform(get("/api/v1/pedidos").header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isForbidden());

        // mismo email otra vez → 409
        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"ana.registro@mail.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isConflict());
    }

    /**
     * Matriz de permisos: rol, método, ruta, ¿permitido?
     * Permitido significa "pasó la seguridad" (luego puede ser 200, 404, 400... según la lógica).
     * Denegado significa exactamente 403.
     */
    @ParameterizedTest(name = "{0} {1} {2} → permitido={3}")
    @CsvSource({
            "GERENTE,  POST,   /api/v1/platos,               true",
            "MESERO,   POST,   /api/v1/platos,               false",
            "COCINERO, POST,   /api/v1/platos,               false",
            "CLIENTE,  GET,    /api/v1/platos,               false",
            "COCINERO, GET,    /api/v1/platos,               true",
            "COCINERO, PATCH,  /api/v1/platos/999/disponible?disponible=false, true",
            "MESERO,   DELETE, /api/v1/platos/999,           false",
            "GERENTE,  DELETE, /api/v1/platos/999,           true",
            "MESERO,   GET,    /api/v1/mesas,                true",
            "COCINERO, GET,    /api/v1/mesas,                false",
            "MESERO,   POST,   /api/v1/mesas,                false",
            "COCINERO, GET,    /api/v1/pedidos,              true",
            "COCINERO, POST,   /api/v1/pedidos,              false",
            "COCINERO, PATCH,  /api/v1/pedidos/999/estado?estado=LISTO, true",
            "MESERO,   GET,    /api/v1/pedidos/999/historial, false",
            "GERENTE,  GET,    /api/v1/pedidos/999/historial, true",
            "MESERO,   GET,    /api/v1/cuentas,              true",
            "COCINERO, GET,    /api/v1/cuentas,              false",
            "CLIENTE,  GET,    /api/v1/cuentas,              false"
    })
    void matrizDePermisos(String rol, String metodo, String ruta, boolean permitido) throws Exception {
        String body = "{\"nombre\":\"Plato de prueba " + rol + metodo + "\",\"precio\":1000,\"categoria\":\"X\","
                + "\"disponible\":true,\"numero\":900,\"capacidad\":2,\"idMesa\":999,"
                + "\"items\":[{\"idPlato\":1,\"cantidad\":1}]}";

        int status = mockMvc.perform(request(HttpMethod.valueOf(metodo), ruta)
                        .header("Authorization", "Bearer " + tokenDe(rol))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getStatus();

        if (permitido) {
            assertNotEquals(401, status, "no debería pedir autenticación");
            assertNotEquals(403, status, "no debería prohibir el acceso");
        } else {
            assertEquals(403, status);
        }
    }
}
