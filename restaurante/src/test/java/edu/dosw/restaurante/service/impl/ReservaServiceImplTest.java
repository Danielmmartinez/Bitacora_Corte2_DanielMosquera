package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.config.ReservaProperties;
import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.exception.SolicitudInvalidaException;
import edu.dosw.restaurante.mapper.ReservaEntityMapperImpl;
import edu.dosw.restaurante.model.domain.*;
import edu.dosw.restaurante.persistence.entity.ReservaEntity;
import edu.dosw.restaurante.repository.ReservaRepository;
import edu.dosw.restaurante.service.ICuentaService;
import edu.dosw.restaurante.service.IMesaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * El Service recibe un reloj fijo: "ahora" es siempre el 15/06/2030 a las 10:00.
 * Se construye a mano (sin @InjectMocks) porque recibe valores que no son mocks: propiedades y reloj.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 6, 15, 10, 0);
    private static final LocalDateTime HOY_19H = AHORA.withHour(19);

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private IMesaService mesaService;

    @Mock
    private ICuentaService cuentaService;

    private ReservaServiceImpl reservaService;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        ReservaProperties propiedades = new ReservaProperties(LocalTime.of(12, 0), LocalTime.of(21, 0), 120);
        reservaService = new ReservaServiceImpl(reservaRepository, new ReservaEntityMapperImpl(),
                mesaService, cuentaService, propiedades, reloj);
    }

    private Mesa mesa(long id, int capacidad) {
        return Mesa.builder().id(id).numero((int) id).capacidad(capacidad)
                .estado(EstadoMesa.DISPONIBLE).cuentaAbierta(false).build();
    }

    private Reserva nueva(long idMesa, LocalDateTime fechaHora, int comensales) {
        return Reserva.builder().idMesa(idMesa).nombreCliente("Ana").emailCliente("ana@mail.com")
                .fechaHora(fechaHora).comensales(comensales).build();
    }

    private ReservaEntity guardada(long id, long idMesa, LocalDateTime fechaHora, EstadoReserva estado) {
        return ReservaEntity.builder().id(id).idMesa(idMesa).nombreCliente("Ana").emailCliente("ana@mail.com")
                .fechaHora(fechaHora).comensales(2).estado(estado).fechaCreacion(AHORA.minusDays(1)).build();
    }

    private void sinCruces() {
        when(reservaRepository.findByIdMesaAndEstadoAndFechaHoraAfterAndFechaHoraBefore(anyLong(), any(), any(), any()))
                .thenReturn(List.of());
    }

    private void saveAsignaId() {
        when(reservaRepository.save(any())).thenAnswer(inv -> {
            ReservaEntity e = inv.getArgument(0);
            if (e.getId() == null) e.setId(1L);
            return e;
        });
    }

    // ── crear ───────────────────────────────────────────────────

    @Test
    void crear_Exito_QuedaConfirmada() {
        when(mesaService.obtenerPorId(2L)).thenReturn(mesa(2, 4));
        sinCruces();
        saveAsignaId();

        Reserva creada = reservaService.crear(nueva(2, HOY_19H, 3));

        assertEquals(1L, creada.getId());
        assertEquals(EstadoReserva.CONFIRMADA, creada.getEstado());
        assertEquals(AHORA, creada.getFechaCreacion());
        // Busca candidatas en (19:00 - 2h, 19:00 + 2h)
        verify(reservaRepository).findByIdMesaAndEstadoAndFechaHoraAfterAndFechaHoraBefore(
                2L, EstadoReserva.CONFIRMADA, HOY_19H.minusHours(2), HOY_19H.plusHours(2));
    }

    @Test
    void crear_ExcedeCapacidad_LanzaEstadoInvalido() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesa(1, 2));

        Reserva r = nueva(1, HOY_19H, 5);
        assertThrows(EstadoInvalidoException.class, () -> reservaService.crear(r));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crear_FueraDeHorario_LanzaEstadoInvalido() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesa(1, 4));

        Reserva tarde = nueva(1, AHORA.withHour(22), 2);
        Reserva temprano = nueva(1, AHORA.plusDays(1).withHour(11), 2);
        assertThrows(EstadoInvalidoException.class, () -> reservaService.crear(tarde));
        assertThrows(EstadoInvalidoException.class, () -> reservaService.crear(temprano));
    }

    @Test
    void crear_ALasVeintiunoEnPunto_SePermite() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesa(1, 4));
        sinCruces();
        saveAsignaId();

        assertDoesNotThrow(() -> reservaService.crear(nueva(1, AHORA.withHour(21), 2)));
    }

    @Test
    void crear_EnElPasado_LanzaEstadoInvalido() {
        when(mesaService.obtenerPorId(1L)).thenReturn(mesa(1, 4));

        Reserva pasada = nueva(1, AHORA.minusDays(1).withHour(19), 2);
        assertThrows(EstadoInvalidoException.class, () -> reservaService.crear(pasada));
    }

    @Test
    void crear_SeCruzaConOtra_LanzaConflicto() {
        when(mesaService.obtenerPorId(2L)).thenReturn(mesa(2, 4));
        when(reservaRepository.findByIdMesaAndEstadoAndFechaHoraAfterAndFechaHoraBefore(anyLong(), any(), any(), any()))
                .thenReturn(List.of(guardada(9, 2, HOY_19H.plusMinutes(30), EstadoReserva.CONFIRMADA)));

        Reserva r = nueva(2, HOY_19H, 2);
        ConflictoException ex = assertThrows(ConflictoException.class, () -> reservaService.crear(r));
        assertTrue(ex.getMessage().contains("19:30"));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crear_MesaNoExiste_PropagaNoEncontrado() {
        when(mesaService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe"));

        Reserva r = nueva(99, HOY_19H, 2);
        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.crear(r));
    }

    // ── consultar / pertenencia ─────────────────────────────────

    @Test
    void obtenerPorId_PersonalVeCualquiera_ClienteSoloLaSuya() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(guardada(1, 2, HOY_19H, EstadoReserva.CONFIRMADA)));

        assertNotNull(reservaService.obtenerPorId(1L, null));
        assertNotNull(reservaService.obtenerPorId(1L, "ANA@mail.com"));
        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.obtenerPorId(1L, "beto@mail.com"));
    }

    @Test
    void obtenerTodas_ConFecha_UsaElRangoDelDia() {
        when(reservaRepository.findEntre(any(), any())).thenReturn(List.of());

        reservaService.obtenerTodas(LocalDate.of(2030, 6, 15));

        verify(reservaRepository).findEntre(LocalDateTime.of(2030, 6, 15, 0, 0), LocalDateTime.of(2030, 6, 16, 0, 0));
    }

    @Test
    void obtenerTodas_SinFecha_TodasOrdenadas() {
        when(reservaRepository.findAll(any(Sort.class))).thenReturn(List.of(guardada(1, 1, HOY_19H, EstadoReserva.CONFIRMADA)));

        assertEquals(1, reservaService.obtenerTodas(null).size());
    }

    @Test
    void obtenerDeCliente_DelegaAlRepositorio() {
        when(reservaRepository.findByEmailClienteIgnoreCaseOrderByFechaHoraDesc("ana@mail.com"))
                .thenReturn(List.of(guardada(1, 1, HOY_19H, EstadoReserva.CONFIRMADA)));

        assertEquals(1, reservaService.obtenerDeCliente("ana@mail.com").size());
    }

    // ── actualizar / cancelar ───────────────────────────────────

    @Test
    void actualizar_NoChocaConsigoMisma() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(guardada(1, 2, HOY_19H, EstadoReserva.CONFIRMADA)));
        when(mesaService.obtenerPorId(2L)).thenReturn(mesa(2, 4));
        // La consulta de cruces devuelve la propia reserva: se debe ignorar
        when(reservaRepository.findByIdMesaAndEstadoAndFechaHoraAfterAndFechaHoraBefore(anyLong(), any(), any(), any()))
                .thenReturn(List.of(guardada(1, 2, HOY_19H, EstadoReserva.CONFIRMADA)));
        saveAsignaId();

        Reserva actualizada = reservaService.actualizar(1L, nueva(2, HOY_19H.plusMinutes(30), 4), "ana@mail.com");

        assertEquals(HOY_19H.plusMinutes(30), actualizada.getFechaHora());
        assertEquals(4, actualizada.getComensales());
    }

    @Test
    void actualizar_ClienteNoCambiaElDueno() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(guardada(1, 2, HOY_19H, EstadoReserva.CONFIRMADA)));
        when(mesaService.obtenerPorId(2L)).thenReturn(mesa(2, 4));
        sinCruces();
        saveAsignaId();

        Reserva datos = nueva(2, HOY_19H, 2);
        datos.setEmailCliente("otro@mail.com");
        assertEquals("ana@mail.com", reservaService.actualizar(1L, datos, "ana@mail.com").getEmailCliente());
    }

    @Test
    void actualizar_Ajena_LanzaNoEncontrado() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(guardada(1, 2, HOY_19H, EstadoReserva.CONFIRMADA)));

        Reserva datos = nueva(2, HOY_19H, 2);
        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.actualizar(1L, datos, "beto@mail.com"));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void cancelar_Vigente_QuedaCancelada() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(guardada(1, 2, HOY_19H, EstadoReserva.CONFIRMADA)));
        saveAsignaId();

        assertEquals(EstadoReserva.CANCELADA, reservaService.cancelar(1L, "ana@mail.com").getEstado());
    }

    @Test
    void cancelar_YaCancelada_O_Pasada_LanzaEstadoInvalido() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(guardada(1, 2, HOY_19H, EstadoReserva.CANCELADA)));
        when(reservaRepository.findById(2L)).thenReturn(Optional.of(guardada(2, 2, AHORA.minusHours(1), EstadoReserva.CONFIRMADA)));

        assertThrows(EstadoInvalidoException.class, () -> reservaService.cancelar(1L, null));
        assertThrows(EstadoInvalidoException.class, () -> reservaService.cancelar(2L, null));
        verify(reservaRepository, never()).save(any());
    }

    // ── llegada ─────────────────────────────────────────────────

    @Test
    void registrarLlegada_Hoy_AbreCuentaYSeCumple() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(guardada(1, 2, HOY_19H, EstadoReserva.CONFIRMADA)));
        when(cuentaService.abrir(2L)).thenReturn(Cuenta.builder().id(7L).idMesa(2L).build());
        saveAsignaId();

        Reserva cumplida = reservaService.registrarLlegada(1L);

        assertEquals(EstadoReserva.CUMPLIDA, cumplida.getEstado());
        assertEquals(7L, cumplida.getIdCuenta());
        ArgumentCaptor<ReservaEntity> captor = ArgumentCaptor.forClass(ReservaEntity.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getIdCuenta());
    }

    @Test
    void registrarLlegada_OtroDia_LanzaEstadoInvalidoYNoAbreCuenta() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(guardada(1, 2, HOY_19H.plusDays(1), EstadoReserva.CONFIRMADA)));

        assertThrows(EstadoInvalidoException.class, () -> reservaService.registrarLlegada(1L));
        verifyNoInteractions(cuentaService);
    }

    @Test
    void registrarLlegada_NoConfirmada_LanzaEstadoInvalido() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(guardada(1, 2, HOY_19H, EstadoReserva.CANCELADA)));

        assertThrows(EstadoInvalidoException.class, () -> reservaService.registrarLlegada(1L));
        verifyNoInteractions(cuentaService);
    }

    @Test
    void registrarLlegada_MesaConCuentaAbierta_PropagaConflicto() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(guardada(1, 2, HOY_19H, EstadoReserva.CONFIRMADA)));
        when(cuentaService.abrir(2L)).thenThrow(new ConflictoException("La mesa ya tiene cuenta"));

        assertThrows(ConflictoException.class, () -> reservaService.registrarLlegada(1L));
        verify(reservaRepository, never()).save(any());
    }

    // ── disponibilidad ──────────────────────────────────────────

    @Test
    void mesasDisponibles_FiltraCapacidadYCrucesYOrdenaPorCapacidad() {
        when(mesaService.obtenerTodas()).thenReturn(List.of(mesa(3, 6), mesa(1, 2), mesa(2, 4), mesa(4, 4)));
        // La mesa 2 ya tiene una reserva que se cruza; la 4 y la 3 están libres
        when(reservaRepository.findByIdMesaAndEstadoAndFechaHoraAfterAndFechaHoraBefore(eq(2L), any(), any(), any()))
                .thenReturn(List.of(guardada(9, 2, HOY_19H, EstadoReserva.CONFIRMADA)));
        when(reservaRepository.findByIdMesaAndEstadoAndFechaHoraAfterAndFechaHoraBefore(eq(3L), any(), any(), any()))
                .thenReturn(List.of());
        when(reservaRepository.findByIdMesaAndEstadoAndFechaHoraAfterAndFechaHoraBefore(eq(4L), any(), any(), any()))
                .thenReturn(List.of());

        List<Mesa> libres = reservaService.mesasDisponibles(HOY_19H, 3);

        assertEquals(List.of(4L, 3L), libres.stream().map(Mesa::getId).toList());
    }

    @Test
    void mesasDisponibles_ParametrosInvalidos() {
        assertThrows(SolicitudInvalidaException.class, () -> reservaService.mesasDisponibles(HOY_19H, 0));
        assertThrows(EstadoInvalidoException.class, () -> reservaService.mesasDisponibles(AHORA.withHour(23), 2));
    }
}
