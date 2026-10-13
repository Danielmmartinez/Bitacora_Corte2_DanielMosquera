package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.config.ParqueaderoProperties;
import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.exception.SolicitudInvalidaException;
import edu.dosw.restaurante.mapper.RegistroVehiculoEntityMapperImpl;
import edu.dosw.restaurante.model.domain.EstadoParqueadero;
import edu.dosw.restaurante.model.domain.RegistroVehiculo;
import edu.dosw.restaurante.model.domain.TipoVehiculo;
import edu.dosw.restaurante.persistence.entity.RegistroVehiculoEntity;
import edu.dosw.restaurante.repository.RegistroVehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParqueaderoServiceImplTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 6, 15, 18, 0);

    @Mock
    private RegistroVehiculoRepository registroRepository;

    private ParqueaderoServiceImpl parqueaderoService;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        // Capacidad 2 para poder probar "lleno" fácilmente
        parqueaderoService = new ParqueaderoServiceImpl(registroRepository, new RegistroVehiculoEntityMapperImpl(),
                new ParqueaderoProperties(2, 5000, 2000), reloj);
    }

    private RegistroVehiculoEntity adentro(String placa, TipoVehiculo tipo, LocalDateTime entrada) {
        return RegistroVehiculoEntity.builder().id(1L).placa(placa).tipo(tipo).entrada(entrada).build();
    }

    private void saveDevuelveLoMismo() {
        when(registroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void entrada_NormalizaPlacaYDetectaCarro() {
        when(registroRepository.existsByPlacaAndSalidaIsNull("ABC123")).thenReturn(false);
        when(registroRepository.countBySalidaIsNull()).thenReturn(0L);
        saveDevuelveLoMismo();

        RegistroVehiculo registro = parqueaderoService.registrarEntrada(" abc-123 ");

        assertEquals("ABC123", registro.getPlaca());
        assertEquals(TipoVehiculo.CARRO, registro.getTipo());
        assertEquals(AHORA, registro.getEntrada());
        assertTrue(registro.estaActivo());
    }

    @Test
    void entrada_DetectaMoto() {
        when(registroRepository.countBySalidaIsNull()).thenReturn(0L);
        saveDevuelveLoMismo();

        assertEquals(TipoVehiculo.MOTO, parqueaderoService.registrarEntrada("xyz45k").getTipo());
    }

    @Test
    void entrada_PlacaInvalida_LanzaSolicitudInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> parqueaderoService.registrarEntrada("12345"));
        verifyNoInteractions(registroRepository);
    }

    @Test
    void entrada_VehiculoYaAdentro_LanzaConflicto() {
        when(registroRepository.existsByPlacaAndSalidaIsNull("ABC123")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> parqueaderoService.registrarEntrada("ABC123"));
        verify(registroRepository, never()).save(any());
    }

    @Test
    void entrada_ParqueaderoLleno_LanzaEstadoInvalido() {
        when(registroRepository.countBySalidaIsNull()).thenReturn(2L);

        assertThrows(EstadoInvalidoException.class, () -> parqueaderoService.registrarEntrada("ABC123"));
        verify(registroRepository, never()).save(any());
    }

    @Test
    void salida_CarroNoventaMinutos_CobraDosHoras() {
        when(registroRepository.findByPlacaAndSalidaIsNull("ABC123"))
                .thenReturn(Optional.of(adentro("ABC123", TipoVehiculo.CARRO, AHORA.minusMinutes(90))));
        saveDevuelveLoMismo();

        RegistroVehiculo salida = parqueaderoService.registrarSalida("abc-123");

        assertEquals(AHORA, salida.getSalida());
        assertEquals(10000.0, salida.getCobro());
        ArgumentCaptor<RegistroVehiculoEntity> captor = ArgumentCaptor.forClass(RegistroVehiculoEntity.class);
        verify(registroRepository).save(captor.capture());
        assertEquals(10000.0, captor.getValue().getCobro());
    }

    @Test
    void salida_MotoUsaSuTarifa() {
        when(registroRepository.findByPlacaAndSalidaIsNull("XYZ45K"))
                .thenReturn(Optional.of(adentro("XYZ45K", TipoVehiculo.MOTO, AHORA.minusMinutes(20))));
        saveDevuelveLoMismo();

        assertEquals(2000.0, parqueaderoService.registrarSalida("XYZ45K").getCobro());
    }

    @Test
    void salida_PlacaNoEstaAdentro_LanzaNoEncontrado() {
        when(registroRepository.findByPlacaAndSalidaIsNull("ABC123")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> parqueaderoService.registrarSalida("ABC123"));
    }

    @Test
    void estado_CalculaCuposDisponibles() {
        when(registroRepository.findBySalidaIsNullOrderByEntradaAsc())
                .thenReturn(List.of(adentro("ABC123", TipoVehiculo.CARRO, AHORA)));

        EstadoParqueadero estado = parqueaderoService.obtenerEstado();

        assertEquals(2, estado.capacidad());
        assertEquals(1, estado.ocupados());
        assertEquals(1, estado.disponibles());
        assertEquals("ABC123", estado.activos().get(0).getPlaca());
    }

    @Test
    void registrosDelDia_SinFecha_UsaHoy() {
        when(registroRepository.findEntradasEntre(any(), any())).thenReturn(List.of());

        parqueaderoService.obtenerRegistrosDelDia(null);

        verify(registroRepository).findEntradasEntre(LocalDate.of(2030, 6, 15).atStartOfDay(),
                LocalDate.of(2030, 6, 16).atStartOfDay());
    }

    @Test
    void obtenerPorId_NoExiste_LanzaNoEncontrado() {
        when(registroRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> parqueaderoService.obtenerPorId(9L));
    }
}
