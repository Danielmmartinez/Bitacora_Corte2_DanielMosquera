package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.model.domain.EstadoReserva;
import edu.dosw.restaurante.persistence.entity.ReservaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ReservaRepositoryTest {

    private static final LocalDateTime SIETE = LocalDateTime.of(2030, 6, 15, 19, 0);

    @Autowired
    private ReservaRepository reservaRepository;

    private ReservaEntity reserva(long idMesa, LocalDateTime fecha, EstadoReserva estado, String email) {
        return ReservaEntity.builder().idMesa(idMesa).nombreCliente("X").emailCliente(email).fechaHora(fecha)
                .comensales(2).estado(estado).fechaCreacion(fecha.minusDays(1)).build();
    }

    @BeforeEach
    void datos() {
        reservaRepository.save(reserva(1, SIETE, EstadoReserva.CONFIRMADA, "ana@mail.com"));
        reservaRepository.save(reserva(1, SIETE.minusHours(2), EstadoReserva.CONFIRMADA, "beto@mail.com")); // 17:00
        reservaRepository.save(reserva(1, SIETE.plusHours(1), EstadoReserva.CANCELADA, "ana@mail.com"));
        reservaRepository.save(reserva(2, SIETE, EstadoReserva.CONFIRMADA, "ana@mail.com"));
        reservaRepository.save(reserva(1, SIETE.plusDays(1), EstadoReserva.CONFIRMADA, "ana@mail.com"));
    }

    @Test
    void candidatasACruce_LimitesExclusivos() {
        // Ventana (17:00, 21:00) para una reserva nueva a las 19:00 con duración 2 h
        List<ReservaEntity> candidatas = reservaRepository.findByIdMesaAndEstadoAndFechaHoraAfterAndFechaHoraBefore(
                1L, EstadoReserva.CONFIRMADA, SIETE.minusHours(2), SIETE.plusHours(2));

        // Solo la de las 19:00: la de las 17:00 termina justo a las 19:00, la cancelada y la de mañana no cuentan
        assertEquals(1, candidatas.size());
        assertEquals(SIETE, candidatas.get(0).getFechaHora());
    }

    @Test
    void findEntre_JPQL_DevuelveElDiaOrdenado() {
        List<ReservaEntity> delDia = reservaRepository.findEntre(SIETE.toLocalDate().atStartOfDay(),
                SIETE.toLocalDate().plusDays(1).atStartOfDay());

        assertEquals(4, delDia.size());
        assertEquals(SIETE.minusHours(2), delDia.get(0).getFechaHora());
    }

    @Test
    void porEmail_IgnoraMayusculasYOrdenaDescendente() {
        List<ReservaEntity> deAna = reservaRepository.findByEmailClienteIgnoreCaseOrderByFechaHoraDesc("ANA@MAIL.COM");

        assertEquals(4, deAna.size());
        assertEquals(SIETE.plusDays(1), deAna.get(0).getFechaHora());
    }
}
