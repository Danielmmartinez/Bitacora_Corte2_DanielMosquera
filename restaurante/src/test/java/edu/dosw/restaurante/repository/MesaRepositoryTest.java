package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.persistence.entity.MesaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class MesaRepositoryTest {

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private TestEntityManager entityManager;

    private MesaEntity mesa(int numero, EstadoMesa estado) {
        return MesaEntity.builder().numero(numero).capacidad(4).estado(estado)
                .cuentaAbierta(estado == EstadoMesa.OCUPADA).build();
    }

    @Test
    void findByEstado_FiltraPorEstado() {
        mesaRepository.save(mesa(1, EstadoMesa.DISPONIBLE));
        mesaRepository.save(mesa(2, EstadoMesa.OCUPADA));
        mesaRepository.save(mesa(3, EstadoMesa.OCUPADA));

        assertEquals(2, mesaRepository.findByEstado(EstadoMesa.OCUPADA).size());
        assertEquals(1, mesaRepository.findByEstado(EstadoMesa.DISPONIBLE).size());
        assertTrue(mesaRepository.findByEstado(EstadoMesa.RESERVADA).isEmpty());
    }

    @Test
    void existsByNumero_Y_ExcluyendoId() {
        MesaEntity m1 = mesaRepository.save(mesa(1, EstadoMesa.DISPONIBLE));
        mesaRepository.save(mesa(2, EstadoMesa.DISPONIBLE));

        assertTrue(mesaRepository.existsByNumero(1));
        assertFalse(mesaRepository.existsByNumero(9));
        assertFalse(mesaRepository.existsByNumeroAndIdNot(1, m1.getId()));
        assertTrue(mesaRepository.existsByNumeroAndIdNot(2, m1.getId()));
    }

    @Test
    void estado_SeGuardaComoTextoEnLaBD() {
        mesaRepository.saveAndFlush(mesa(4, EstadoMesa.OCUPADA));

        Object valorEnColumna = entityManager.getEntityManager()
                .createNativeQuery("SELECT estado FROM mesas WHERE numero = 4")
                .getSingleResult();

        assertEquals("OCUPADA", valorEnColumna);
    }
}
