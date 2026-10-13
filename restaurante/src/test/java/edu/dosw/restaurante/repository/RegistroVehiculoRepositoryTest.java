package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.model.domain.EstadoCuenta;
import edu.dosw.restaurante.model.domain.MetodoPago;
import edu.dosw.restaurante.model.domain.TipoVehiculo;
import edu.dosw.restaurante.persistence.entity.CuentaEntity;
import edu.dosw.restaurante.persistence.entity.RegistroVehiculoEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consultas por rango de fechas que usan el parqueadero y los reportes.
 */
@DataJpaTest
class RegistroVehiculoRepositoryTest {

    private static final LocalDate HOY = LocalDate.of(2030, 6, 15);

    @Autowired
    private RegistroVehiculoRepository registroRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    private RegistroVehiculoEntity registro(String placa, LocalDateTime entrada, LocalDateTime salida) {
        return RegistroVehiculoEntity.builder().placa(placa).tipo(TipoVehiculo.CARRO)
                .entrada(entrada).salida(salida).cobro(salida == null ? null : 5000.0).build();
    }

    @Test
    void activos_SonLosQueNoTienenSalida() {
        registroRepository.save(registro("AAA111", HOY.atTime(12, 0), null));
        registroRepository.save(registro("BBB222", HOY.atTime(11, 0), null));
        registroRepository.save(registro("CCC333", HOY.atTime(9, 0), HOY.atTime(10, 0)));

        assertEquals(2, registroRepository.countBySalidaIsNull());
        assertTrue(registroRepository.existsByPlacaAndSalidaIsNull("AAA111"));
        assertFalse(registroRepository.existsByPlacaAndSalidaIsNull("CCC333"));
        assertTrue(registroRepository.findByPlacaAndSalidaIsNull("CCC333").isEmpty());
        assertEquals("BBB222", registroRepository.findBySalidaIsNullOrderByEntradaAsc().get(0).getPlaca());
    }

    @Test
    void rangos_IncluyenElInicioYExcluyenElFin() {
        registroRepository.save(registro("AAA111", HOY.atStartOfDay(), HOY.atTime(23, 59)));
        registroRepository.save(registro("BBB222", HOY.minusDays(1).atTime(23, 0), HOY.plusDays(1).atStartOfDay()));

        var hoyInicio = HOY.atStartOfDay();
        var mananaInicio = HOY.plusDays(1).atStartOfDay();
        assertEquals(1, registroRepository.findEntradasEntre(hoyInicio, mananaInicio).size());
        assertEquals(1, registroRepository.findSalidasEntre(hoyInicio, mananaInicio).size());
    }

    @Test
    void cuentasCerradasEntre_IgnoraLasAbiertas() {
        cuentaRepository.save(CuentaEntity.builder().idMesa(1L).estado(EstadoCuenta.CERRADA).total(1000.0)
                .metodoPago(MetodoPago.EFECTIVO).fechaApertura(HOY.atTime(12, 0)).fechaCierre(HOY.atTime(13, 0)).build());
        cuentaRepository.save(CuentaEntity.builder().idMesa(2L).estado(EstadoCuenta.ABIERTA)
                .fechaApertura(HOY.atTime(12, 0)).build());

        assertEquals(1, cuentaRepository.findCerradasEntre(HOY.atStartOfDay(), HOY.plusDays(1).atStartOfDay()).size());
    }
}
