package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.persistence.entity.PlatoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @DataJpaTest levanta solo la capa JPA con una H2 en memoria. Cada test corre en una
 * transacción que se revierte al terminar, así que los tests no se afectan entre sí.
 */
@DataJpaTest
class PlatoRepositoryTest {

    @Autowired
    private PlatoRepository platoRepository;

    private PlatoEntity nigiri;

    @BeforeEach
    void setUp() {
        nigiri = platoRepository.save(plato("Nigiri", "Sushi", true));
        platoRepository.save(plato("Sashimi", "sushi", false));
        platoRepository.save(plato("Miso", "Sopas", true));
    }

    private PlatoEntity plato(String nombre, String categoria, boolean disponible) {
        return PlatoEntity.builder().nombre(nombre).precio(10000.0).categoria(categoria).disponible(disponible).build();
    }

    @Test
    void save_AsignaIdGeneradoPorLaBD() {
        assertNotNull(nigiri.getId());
        assertTrue(platoRepository.findById(nigiri.getId()).isPresent());
    }

    @Test
    void findByDisponibleTrue_SoloDisponibles() {
        List<PlatoEntity> disponibles = platoRepository.findByDisponibleTrue();
        assertEquals(2, disponibles.size());
        assertTrue(disponibles.stream().allMatch(PlatoEntity::getDisponible));
    }

    @Test
    void findByDisponibleTrueAndCategoriaIgnoreCase_FiltraAmbos() {
        List<PlatoEntity> sushi = platoRepository.findByDisponibleTrueAndCategoriaIgnoreCase("SUSHI");
        assertEquals(1, sushi.size());
        assertEquals("Nigiri", sushi.get(0).getNombre());
    }

    @Test
    void existsByNombreIgnoreCase_NoDistingueMayusculas() {
        assertTrue(platoRepository.existsByNombreIgnoreCase("nIgIrI"));
        assertFalse(platoRepository.existsByNombreIgnoreCase("Ramen"));
    }

    @Test
    void existsByNombreIgnoreCaseAndIdNot_ExcluyeElPropioPlato() {
        assertFalse(platoRepository.existsByNombreIgnoreCaseAndIdNot("Nigiri", nigiri.getId()));
        assertTrue(platoRepository.existsByNombreIgnoreCaseAndIdNot("Miso", nigiri.getId()));
    }

    @Test
    void nombreDuplicado_LaBDLoRechazaPorLaRestriccionUnique() {
        PlatoEntity duplicado = plato("Nigiri", "Sushi", true);
        assertThrows(DataIntegrityViolationException.class, () -> platoRepository.saveAndFlush(duplicado));
    }
}
