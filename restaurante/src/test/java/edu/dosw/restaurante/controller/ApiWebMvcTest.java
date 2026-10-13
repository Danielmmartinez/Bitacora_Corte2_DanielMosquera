package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.config.CorsConfig;
import edu.dosw.restaurante.config.SecurityConfig;
import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.mapper.CuentaMapperImpl;
import edu.dosw.restaurante.mapper.EventoPedidoMapperImpl;
import edu.dosw.restaurante.mapper.MesaMapperImpl;
import edu.dosw.restaurante.mapper.PedidoMapperImpl;
import edu.dosw.restaurante.mapper.PlatoMapperImpl;
import edu.dosw.restaurante.mapper.UsuarioMapperImpl;
import edu.dosw.restaurante.model.domain.Cuenta;
import edu.dosw.restaurante.model.domain.EstadoCuenta;
import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.domain.EventoPedido;
import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.model.domain.MetodoPago;
import edu.dosw.restaurante.model.domain.Plato;
import edu.dosw.restaurante.model.domain.TipoEventoPedido;
import edu.dosw.restaurante.model.domain.Usuario;
import edu.dosw.restaurante.security.JwtUtil;
import edu.dosw.restaurante.security.RespuestasSeguridad;
import edu.dosw.restaurante.security.Rol;
import edu.dosw.restaurante.security.Sesion;
import edu.dosw.restaurante.service.IAuthService;
import edu.dosw.restaurante.service.ICuentaService;
import edu.dosw.restaurante.service.IEventoPedidoService;
import edu.dosw.restaurante.service.IMesaService;
import edu.dosw.restaurante.service.IPedidoService;
import edu.dosw.restaurante.service.IPlatoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas a nivel HTTP: rutas, verbos, @Valid, códigos de estado, formato del ErrorResponseDTO
 * y permisos por rol. Los servicios se mockean; los mappers y la configuración de seguridad son los reales.
 * @WithMockUser simula un usuario ya autenticado con ese rol (sin generar un JWT de verdad).
 */
@WebMvcTest(controllers = {PlatoController.class, MenuController.class, MesaController.class,
        PedidoController.class, CuentaController.class, AuthController.class})
@Import({PlatoMapperImpl.class, MesaMapperImpl.class, PedidoMapperImpl.class, EventoPedidoMapperImpl.class,
        CuentaMapperImpl.class, UsuarioMapperImpl.class,
        SecurityConfig.class, CorsConfig.class, RespuestasSeguridad.class})
@WithMockUser(roles = "GERENTE") // por defecto cada test corre como gerente
class ApiWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IPlatoService platoService;

    @MockBean
    private IMesaService mesaService;

    @MockBean
    private IPedidoService pedidoService;

    @MockBean
    private IEventoPedidoService eventoService;

    @MockBean
    private ICuentaService cuentaService;

    @MockBean
    private IAuthService authService;

    // Dependencias del JwtAuthFilter (aquí no se usan: @WithMockUser ya deja al usuario autenticado)
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    private static final String PLATO_VALIDO =
            "{\"nombre\":\"California Roll\",\"precio\":18000,\"categoria\":\"Rolls\",\"disponible\":true}";

    // ── Platos ──────────────────────────────────────────────────

    @Test
    void postPlato_Valido_Devuelve201ConBody() throws Exception {
        when(platoService.crear(any())).thenAnswer(inv -> {
            Plato p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        mockMvc.perform(post("/api/v1/platos").contentType(MediaType.APPLICATION_JSON).content(PLATO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("California Roll"));
    }

    @Test
    void postPlato_BodyInvalido_Devuelve400ConErroresPorCampo() throws Exception {
        mockMvc.perform(post("/api/v1/platos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"precio\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.nombre").exists())
                .andExpect(jsonPath("$.errors.precio").exists())
                .andExpect(jsonPath("$.errors.categoria").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/platos"));
        verifyNoInteractions(platoService);
    }

    @Test
    void postPlato_JsonMalFormado_Devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/platos").contentType(MediaType.APPLICATION_JSON).content("{nombre:"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postPlato_NombreDuplicado_Devuelve409() throws Exception {
        when(platoService.crear(any())).thenThrow(new ConflictoException("Ya existe"));

        mockMvc.perform(post("/api/v1/platos").contentType(MediaType.APPLICATION_JSON).content(PLATO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe"));
    }

    @Test
    void getPlato_NoExiste_Devuelve404SinCampoErrors() throws Exception {
        when(platoService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe"));

        mockMvc.perform(get("/api/v1/platos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No existe"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void getPlato_IdNoNumerico_Devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/platos/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putPlato_Valido_Devuelve200() throws Exception {
        when(platoService.actualizar(eq(1L), any())).thenAnswer(inv -> inv.getArgument(1));

        mockMvc.perform(put("/api/v1/platos/1").contentType(MediaType.APPLICATION_JSON).content(PLATO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("California Roll"));
    }

    @Test
    void patchDisponibilidad_Devuelve200() throws Exception {
        when(platoService.cambiarDisponibilidad(1L, false))
                .thenReturn(Plato.builder().id(1L).nombre("Gyoza").disponible(false).build());

        mockMvc.perform(patch("/api/v1/platos/1/disponible").param("disponible", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false));
    }

    @Test
    void patchDisponibilidad_SinParametro_Devuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/platos/1/disponible"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro 'disponible' es obligatorio"));
    }

    @Test
    void deletePlato_Devuelve204SinBody() throws Exception {
        mockMvc.perform(delete("/api/v1/platos/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(platoService).eliminar(1L);
    }

    @Test
    void metodoNoSoportado_Devuelve405() throws Exception {
        mockMvc.perform(patch("/api/v1/platos"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ── Menú ────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    void getMenu_Devuelve200() throws Exception {
        when(platoService.obtenerDisponibles())
                .thenReturn(List.of(Plato.builder().id(1L).nombre("Nigiri").disponible(true).build()));

        mockMvc.perform(get("/api/v1/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getMenuDetalle_PlatoAgotado_Devuelve404() throws Exception {
        when(platoService.obtenerDisponiblePorId(1L)).thenThrow(new RecursoNoEncontradoException("No disponible"));

        mockMvc.perform(get("/api/v1/menu/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMenuPorCategoria_Devuelve200() throws Exception {
        when(platoService.obtenerDisponiblesPorCategoria("Rolls")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/menu/categoria/Rolls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── Mesas ───────────────────────────────────────────────────

    @Test
    void getMesas_FiltroPorEstado_Devuelve200() throws Exception {
        when(mesaService.obtenerPorEstado(EstadoMesa.DISPONIBLE)).thenReturn(List.of(
                Mesa.builder().id(1L).numero(1).capacidad(4).estado(EstadoMesa.DISPONIBLE).cuentaAbierta(false).build()));

        mockMvc.perform(get("/api/v1/mesas").param("estado", "DISPONIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("DISPONIBLE"));
    }

    @Test
    void getMesas_EstadoInexistente_Devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/mesas").param("estado", "VOLANDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postMesa_CapacidadCero_Devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/mesas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":1,\"capacidad\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.capacidad").exists());
    }

    @Test
    void putMesa_Valida_Devuelve200() throws Exception {
        when(mesaService.actualizar(eq(1L), any())).thenReturn(
                Mesa.builder().id(1L).numero(9).capacidad(6).estado(EstadoMesa.DISPONIBLE).cuentaAbierta(false).build());

        mockMvc.perform(put("/api/v1/mesas/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":9,\"capacidad\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(9));
    }

    @Test
    void patchEstadoMesa_ReglaDeNegocio_Devuelve422() throws Exception {
        when(mesaService.cambiarEstado(1L, EstadoMesa.RESERVADA)).thenThrow(new EstadoInvalidoException("Cuenta abierta"));

        mockMvc.perform(patch("/api/v1/mesas/1/estado").param("estado", "RESERVADA"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deleteMesa_ConCuentaAbierta_Devuelve409() throws Exception {
        doThrow(new ConflictoException("Cuenta abierta")).when(mesaService).eliminar(1L);

        mockMvc.perform(delete("/api/v1/mesas/1"))
                .andExpect(status().isConflict());
    }

    // ── Pedidos ─────────────────────────────────────────────────

    @Test
    void postPedido_SinItems_Devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMesa\":1,\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.items").exists());
    }

    @Test
    void postPedido_ItemInvalido_Devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMesa\":1,\"items\":[{\"idPlato\":1,\"cantidad\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].cantidad']").exists());
    }

    @Test
    void postPedido_Valido_Devuelve201ConTotal() throws Exception {
        when(pedidoService.crear(any())).thenAnswer(inv -> {
            var p = (edu.dosw.restaurante.model.domain.Pedido) inv.getArgument(0);
            p.setId(1L);
            p.getItems().forEach(i -> i.setPrecioCongelado(10000.0));
            return p;
        });

        mockMvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMesa\":1,\"items\":[{\"idPlato\":1,\"cantidad\":3}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RECIBIDO"))
                .andExpect(jsonPath("$.total").value(30000.0));
    }

    @Test
    void patchEstadoPedido_TransicionInvalida_Devuelve422() throws Exception {
        when(pedidoService.cambiarEstado(1L, EstadoPedido.RECIBIDO))
                .thenThrow(new EstadoInvalidoException("Transición inválida"));

        mockMvc.perform(patch("/api/v1/pedidos/1/estado").param("estado", "RECIBIDO"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Transición inválida"));
    }

    @Test
    void patchEstadoPedido_EstadoInexistente_Devuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/pedidos/1/estado").param("estado", "COCINADO"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void getPedidos_FiltroPorEstado_Devuelve200() throws Exception {
        when(pedidoService.obtenerPorEstado(EstadoPedido.EN_PREPARACION)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pedidos").param("estado", "EN_PREPARACION"))
                .andExpect(status().isOk());
        verify(pedidoService).obtenerPorEstado(EstadoPedido.EN_PREPARACION);
    }

    @Test
    void deletePedido_YaEnCocina_Devuelve422() throws Exception {
        doThrow(new EstadoInvalidoException("Ya en cocina")).when(pedidoService).eliminar(1L);

        mockMvc.perform(delete("/api/v1/pedidos/1"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getHistorial_Devuelve200ConEventos() throws Exception {
        when(eventoService.obtenerHistorial(1L)).thenReturn(List.of(EventoPedido.builder()
                .id("abc123").idPedido(1L).tipo(TipoEventoPedido.CAMBIO_ESTADO)
                .estadoAnterior(EstadoPedido.RECIBIDO).estadoNuevo(EstadoPedido.EN_PREPARACION)
                .fecha(LocalDateTime.now()).detalle(Map.of()).build()));

        mockMvc.perform(get("/api/v1/pedidos/1/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("CAMBIO_ESTADO"))
                .andExpect(jsonPath("$[0].estadoNuevo").value("EN_PREPARACION"));
    }

    @Test
    void getHistorial_MongoCaido_Devuelve503() throws Exception {
        when(eventoService.obtenerHistorial(1L))
                .thenThrow(new DataAccessResourceFailureException("Timed out connecting to localhost:27017"));

        mockMvc.perform(get("/api/v1/pedidos/1/historial"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    // ── Cuentas ─────────────────────────────────────────────────

    @Test
    void postCuenta_Devuelve201() throws Exception {
        when(cuentaService.abrir(1L)).thenReturn(Cuenta.builder().id(5L).idMesa(1L)
                .estado(EstadoCuenta.ABIERTA).fechaApertura(LocalDateTime.now()).build());

        mockMvc.perform(post("/api/v1/cuentas").contentType(MediaType.APPLICATION_JSON).content("{\"idMesa\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.total").value(0.0));
    }

    @Test
    void postCuenta_SinMesa_Devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/cuentas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.idMesa").exists());
    }

    @Test
    void pagar_Valido_Devuelve200ConCambio() throws Exception {
        Cuenta pagada = Cuenta.builder().id(5L).idMesa(1L).estado(EstadoCuenta.CERRADA)
                .total(44000.0).montoRecibido(50000.0).metodoPago(MetodoPago.EFECTIVO).build();
        when(cuentaService.pagar(5L, MetodoPago.EFECTIVO, 50000.0)).thenReturn(pagada);

        mockMvc.perform(post("/api/v1/cuentas/5/pago").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"EFECTIVO\",\"montoRecibido\":50000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.cambio").value(6000.0));
    }

    @Test
    void pagar_MetodoInexistente_Devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/cuentas/5/pago").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"BITCOIN\",\"montoRecibido\":50000}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cuentaService);
    }

    @Test
    void pagar_ReglaDeNegocio_Devuelve422() throws Exception {
        when(cuentaService.pagar(5L, MetodoPago.TARJETA, 1.0)).thenThrow(new EstadoInvalidoException("Pedidos pendientes"));

        mockMvc.perform(post("/api/v1/cuentas/5/pago").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\":\"TARJETA\",\"montoRecibido\":1}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void patchSolicitarCuenta_Devuelve200() throws Exception {
        when(cuentaService.solicitarPago(5L)).thenReturn(Cuenta.builder().id(5L).estado(EstadoCuenta.EN_PAGO).build());

        mockMvc.perform(patch("/api/v1/cuentas/5/solicitar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PAGO"));
    }

    // ── Autenticación ───────────────────────────────────────────

    @Test
    @WithAnonymousUser
    void login_Valido_DevuelveToken() throws Exception {
        when(authService.login("mesero@sakura.com", "Mesero123!")).thenReturn(new Sesion("abc.def.ghi", 3600,
                Usuario.builder().id(2L).email("mesero@sakura.com").nombre("Mesero").rol(Rol.MESERO).build()));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mesero@sakura.com\",\"password\":\"Mesero123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("abc.def.ghi"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("MESERO"));
    }

    @Test
    @WithAnonymousUser
    void login_CredencialesIncorrectas_Devuelve401() throws Exception {
        when(authService.login(any(), any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mesero@sakura.com\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    @WithAnonymousUser
    void registro_PasswordCorta_Devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"ana@mail.com\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    // ── Permisos por rol ────────────────────────────────────────

    @Test
    @WithAnonymousUser
    void sinToken_RutaProtegida_Devuelve401EnJson() throws Exception {
        mockMvc.perform(get("/api/v1/platos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/platos"));
        verifyNoInteractions(platoService);
    }

    @Test
    @WithMockUser(roles = "MESERO")
    void mesero_NoPuedeCrearPlatos_Devuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/platos").contentType(MediaType.APPLICATION_JSON).content(PLATO_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permiso para realizar esta acción"));
        verifyNoInteractions(platoService);
    }

    @Test
    @WithMockUser(roles = "COCINERO")
    void cocinero_PuedeMarcarPlatoAgotado() throws Exception {
        when(platoService.cambiarDisponibilidad(1L, false)).thenReturn(Plato.builder().id(1L).disponible(false).build());

        mockMvc.perform(patch("/api/v1/platos/1/disponible").param("disponible", "false"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COCINERO")
    void cocinero_NoPuedeVerCuentas_Devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/cuentas")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void cliente_NoPuedeVerPedidos_Devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MESERO")
    void mesero_NoPuedeVerHistorial_Devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos/1/historial")).andExpect(status().isForbidden());
        verifyNoInteractions(eventoService);
    }

    // ── CORS ────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    void cors_OrigenPermitido_RespondeConCabecera() throws Exception {
        mockMvc.perform(options("/api/v1/platos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @WithAnonymousUser
    void cors_OrigenNoPermitido_Rechazado() throws Exception {
        mockMvc.perform(options("/api/v1/platos")
                        .header("Origin", "http://sitio-malicioso.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void errorInesperado_Devuelve500SinFiltrarMensaje() throws Exception {
        when(pedidoService.obtenerTodos()).thenThrow(new IllegalStateException("detalle interno secreto"));

        mockMvc.perform(get("/api/v1/pedidos"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error interno no esperado"));
    }
}
