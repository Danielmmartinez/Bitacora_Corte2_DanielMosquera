package edu.dosw.restaurante;

import edu.dosw.restaurante.model.domain.Plato;
import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.service.PlatoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PlatoServiceImplTest {

    @InjectMocks
    private PlatoServiceImpl platoService;

    private Plato platoEjemplo;

    @BeforeEach
    void setUp() {
        platoEjemplo = Plato.builder()
                .nombre("Hamburguesa Artesanal")
                .precio(25000.0)
                .categoria("PLATO_FUERTE")
                .disponible(true)
                .build();
    }

    @Test
    @DisplayName("1. Happy Path: Crear y consultar plato exitosamente")
    void crearYObtenerPorId_Exito() {
        Plato creado = platoService.crear(platoEjemplo);

        assertNotNull(creado.getId());
        assertEquals("Hamburguesa Artesanal", creado.getNombre());

        Plato encontrado = platoService.obtenerPorId(creado.getId());
        assertEquals(creado.getId(), encontrado.getId());
    }

    @Test
    @DisplayName("2. Recurso no encontrado: Buscar ID inexistente lanza RecursoNoEncontradoException")
    void obtenerPorId_Inexistente_LanzaExcepcion() {
        Long idInexistente = 999L;

        assertThrows(RecursoNoEncontradoException.class, () -> {
            platoService.obtenerPorId(idInexistente);
        });
    }

    @Test
    @DisplayName("3. Conflicto: Crear plato con nombre duplicado lanza ConflictoException")
    void crear_NombreDuplicado_LanzaConflictoException() {
        platoService.crear(platoEjemplo);

        Plato duplicado = Plato.builder()
                .nombre("Hamburguesa Artesanal")
                .precio(30000.0)
                .categoria("PLATO_FUERTE")
                .disponible(true)
                .build();

        assertThrows(ConflictoException.class, () -> {
            platoService.crear(duplicado);
        });
    }

    @Test
    @DisplayName("5. Lista vacía: Obtener todos sin registros devuelve lista vacía")
    void obtenerTodos_SinRegistros_DevuelveListaVacia() {
        List<Plato> resultado = platoService.obtenerTodos();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }
}