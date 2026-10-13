package edu.dosw.restaurante.controller;

import edu.dosw.restaurante.config.CorsConfig;
import edu.dosw.restaurante.config.SecurityConfig;
import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.SolicitudInvalidaException;
import edu.dosw.restaurante.mapper.MesaMapperImpl;
import edu.dosw.restaurante.mapper.ParqueaderoMapperImpl;
import edu.dosw.restaurante.mapper.ReporteMapperImpl;
import edu.dosw.restaurante.mapper.ReservaMapperImpl;
import edu.dosw.restaurante.model.domain.*;
import edu.dosw.restaurante.model.domain.reporte.ResumenDia;
import edu.dosw.restaurante.security.JwtUtil;
import edu.dosw.restaurante.security.RespuestasSeguridad;
import edu.dosw.restaurante.service.IParqueaderoService;
import edu.dosw.restaurante.service.IReporteService;
import edu.dosw.restaurante.service.IReservaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Capa HTTP de Reservas, Parqueadero y Reportes: rutas, validaciones, formato de fechas y permisos.
 */
@WebMvcTest(controllers = {ReservaController.class, ParqueaderoController.class, ReporteController.class})
@Import({ReservaMapperImpl.class, MesaMapperImpl.class, ParqueaderoMapperImpl.class, ReporteMapperImpl.class,
        SecurityConfig.class, CorsConfig.class, RespuestasSeguridad.class})
class OperacionWebMvcTest {

    private static final String RESERVA_VALIDA =
            "{\"idMesa\":2,\"nombreCliente\":\"Ana\",\"emailCliente\":\"otra@mail.com\",\"fechaHora\":\"2030-06-15T19:00:00\",\"comensales\":3}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean private IReservaService reservaService;
    @MockBean private IParqueaderoService parqueaderoService;
    @MockBean private IReporteService reporteService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsService userDetailsService;

    private Reserva reserva(String email) {
        return Reserva.builder().id(1L).idMesa(2L).nombreCliente("Ana").emailCliente(email)
                .fechaHora(LocalDateTime.of(2030, 6, 15, 19, 0)).comensales(3).estado(EstadoReserva.CONFIRMADA).build();
    }

    // ── Reservas: quién ve qué ──────────────────────────────────

    @Test
    @WithMockUser(username = "ana@mail.com", roles = "CLIENTE")
    void cliente_ListarReservas_SoloLasSuyas() throws Exception {
        when(reservaService.obtenerDeCliente("ana@mail.com")).thenReturn(List.of(reserva("ana@mail.com")));

        mockMvc.perform(get("/api/v1/reservas").param("fecha", "2030-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].emailCliente").value("ana@mail.com"));
        verify(reservaService, never()).obtenerTodas(any());
    }

    @Test
    @WithMockUser(roles = "MESERO")
    void mesero_ListarReservas_TodasDelDia() throws Exception {
        when(reservaService.obtenerTodas(LocalDate.of(2030, 6, 15))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reservas").param("fecha", "2030-06-15")).andExpect(status().isOk());
        verify(reservaService, never()).obtenerDeCliente(any());
    }

    @Test
    @WithMockUser(username = "ana@mail.com", roles = "CLIENTE")
    void cliente_ObtenerPorId_PasaSuEmail() throws Exception {
        when(reservaService.obtenerPorId(1L, "ana@mail.com")).thenReturn(reserva("ana@mail.com"));

        mockMvc.perform(get("/api/v1/reservas/1")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerente_ObtenerPorId_SinRestriccion() throws Exception {
        when(reservaService.obtenerPorId(eq(1L), isNull())).thenReturn(reserva("ana@mail.com"));

        mockMvc.perform(get("/api/v1/reservas/1")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "ana@mail.com", roles = "CLIENTE")
    void cliente_CrearReserva_QuedaASuNombreAunqueMandeOtroEmail() throws Exception {
        when(reservaService.crear(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(RESERVA_VALIDA))
                .andExpect(status().isCreated());

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaService).crear(captor.capture());
        assertEquals("ana@mail.com", captor.getValue().getEmailCliente());
    }

    @Test
    @WithMockUser(roles = "MESERO")
    void mesero_CrearReserva_RespetaElEmailDelBody() throws Exception {
        when(reservaService.crear(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(RESERVA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailCliente").value("otra@mail.com"));
    }

    @Test
    @WithMockUser(roles = "MESERO")
    void crearReserva_DatosInvalidos_Devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMesa\":2,\"nombreCliente\":\"\",\"fechaHora\":\"2020-01-01T19:00:00\",\"comensales\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombreCliente").exists())
                .andExpect(jsonPath("$.errors.fechaHora").exists())
                .andExpect(jsonPath("$.errors.comensales").exists());
        verifyNoInteractions(reservaService);
    }

    @Test
    @WithMockUser(roles = "MESERO")
    void crearReserva_Cruce_Devuelve409() throws Exception {
        when(reservaService.crear(any())).thenThrow(new ConflictoException("La mesa 2 ya está reservada a las 19:30"));

        mockMvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON).content(RESERVA_VALIDA))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void disponibilidad_ParseaFechaHora() throws Exception {
        when(reservaService.mesasDisponibles(LocalDateTime.of(2030, 6, 15, 19, 30), 4))
                .thenReturn(List.of(Mesa.builder().id(3L).numero(3).capacidad(4).estado(EstadoMesa.DISPONIBLE).build()));

        mockMvc.perform(get("/api/v1/reservas/disponibilidad")
                        .param("fechaHora", "2030-06-15T19:30:00").param("comensales", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value(3));
    }

    @Test
    @WithMockUser(username = "ana@mail.com", roles = "CLIENTE")
    void cliente_Cancelar_PasaSuEmail() throws Exception {
        Reserva cancelada = reserva("ana@mail.com");
        cancelada.cancelar();
        when(reservaService.cancelar(1L, "ana@mail.com")).thenReturn(cancelada);

        mockMvc.perform(patch("/api/v1/reservas/1/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void cliente_NoPuedeRegistrarLlegada_403() throws Exception {
        mockMvc.perform(patch("/api/v1/reservas/1/llegada")).andExpect(status().isForbidden());
        verifyNoInteractions(reservaService);
    }

    @Test
    @WithMockUser(roles = "COCINERO")
    void cocinero_NoVeReservas_403() throws Exception {
        mockMvc.perform(get("/api/v1/reservas")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MESERO")
    void mesero_RegistrarLlegada_DevuelveCuenta() throws Exception {
        Reserva cumplida = reserva("ana@mail.com");
        cumplida.registrarLlegada();
        cumplida.setIdCuenta(7L);
        when(reservaService.registrarLlegada(1L)).thenReturn(cumplida);

        mockMvc.perform(patch("/api/v1/reservas/1/llegada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CUMPLIDA"))
                .andExpect(jsonPath("$.idCuenta").value(7));
    }

    // ── Parqueadero ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "MESERO")
    void entrada_Valida_Devuelve201() throws Exception {
        when(parqueaderoService.registrarEntrada("abc-123")).thenReturn(RegistroVehiculo.builder().id(1L)
                .placa("ABC123").tipo(TipoVehiculo.CARRO).entrada(LocalDateTime.of(2030, 6, 15, 18, 0)).build());

        mockMvc.perform(post("/api/v1/parqueadero/entrada").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placa\":\"abc-123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value("ABC123"))
                .andExpect(jsonPath("$.tipo").value("CARRO"));
    }

    @Test
    @WithMockUser(roles = "MESERO")
    void entrada_PlacaConFormatoInvalido_Devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/parqueadero/entrada").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placa\":\"12-ABCD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.placa").exists());
        verifyNoInteractions(parqueaderoService);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void salida_Devuelve200ConCobro() throws Exception {
        when(parqueaderoService.registrarSalida("ABC123")).thenReturn(RegistroVehiculo.builder().id(1L).placa("ABC123")
                .tipo(TipoVehiculo.CARRO).entrada(LocalDateTime.of(2030, 6, 15, 18, 0))
                .salida(LocalDateTime.of(2030, 6, 15, 19, 30)).cobro(10000.0).build());

        mockMvc.perform(post("/api/v1/parqueadero/salida/ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cobro").value(10000.0));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void estado_Devuelve200() throws Exception {
        when(parqueaderoService.obtenerEstado()).thenReturn(new EstadoParqueadero(20, 1, 19, List.of(
                RegistroVehiculo.builder().placa("ABC123").tipo(TipoVehiculo.CARRO).build())));

        mockMvc.perform(get("/api/v1/parqueadero/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibles").value(19))
                .andExpect(jsonPath("$.activos[0].placa").value("ABC123"));
    }

    @Test
    @WithMockUser(roles = "COCINERO")
    void cocinero_NoUsaElParqueadero_403() throws Exception {
        mockMvc.perform(get("/api/v1/parqueadero/estado")).andExpect(status().isForbidden());
    }

    // ── Reportes ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "GERENTE")
    void resumen_ClavesDeEnumComoTexto() throws Exception {
        when(reporteService.resumenDelDia(LocalDate.of(2030, 6, 15))).thenReturn(new ResumenDia(
                LocalDate.of(2030, 6, 15), 3, Map.of(EstadoPedido.ENTREGADO, 2L, EstadoPedido.CANCELADO, 1L),
                "Ramen", 2, 78000, 39000, 1, 1, 5000, 1, 3, 0));

        mockMvc.perform(get("/api/v1/reportes/resumen").param("fecha", "2030-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidosPorEstado.ENTREGADO").value(2))
                .andExpect(jsonPath("$.ticketPromedio").value(39000.0))
                .andExpect(jsonPath("$.platoMasVendido").value("Ramen"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void platosMasVendidos_LimitePorDefectoCinco() throws Exception {
        when(reporteService.platosMasVendidos(null, null, 5)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reportes/platos-mas-vendidos")).andExpect(status().isOk());
        verify(reporteService).platosMasVendidos(null, null, 5);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void ingresos_RangoInvalido_Devuelve400() throws Exception {
        when(reporteService.ingresos(any(), any())).thenThrow(new SolicitudInvalidaException("desde > hasta"));

        mockMvc.perform(get("/api/v1/reportes/ingresos").param("desde", "2030-06-15").param("hasta", "2030-06-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("desde > hasta"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void fechaMalFormada_Devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/resumen").param("fecha", "15/06/2030"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reporteService);
    }

    @Test
    @WithMockUser(roles = "MESERO")
    void mesero_NoVeReportes_403() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/resumen")).andExpect(status().isForbidden());
    }
}
