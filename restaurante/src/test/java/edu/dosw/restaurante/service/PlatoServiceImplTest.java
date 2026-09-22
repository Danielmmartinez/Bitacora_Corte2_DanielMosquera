package edu.dosw.restaurante.service;

import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.model.domain.Plato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlatoServiceImplTest {

    private PlatoServiceImpl platoService;

    @BeforeEach
    void setUp() {
        platoService = new PlatoServiceImpl();
    }

    @Test
    void crearYObtenerPlato_Exito() {
        Plato plato = Plato.builder().nombre("Pasta").precio(25000.0).categoria("Fuerte").disponible(true).build();
        Plato creado = platoService.crear(plato);

        assertNotNull(creado.getId());
        assertEquals("Pasta", creado.getNombre());

        Long id = creado.getId();
        Plato encontrado = platoService.obtenerPorId(id);
        assertEquals(id, encontrado.getId());
    }

    @Test
    void crearPlato_NombreDuplicado_LanzaConflicto() {
        Plato plato1 = Plato.builder().nombre("Sopa").precio(10000.0).categoria("Entrada").disponible(true).build();
        platoService.crear(plato1);

        Plato plato2 = Plato.builder().nombre("sopa").precio(12000.0).categoria("Entrada").disponible(true).build();
        assertThrows(ConflictoException.class, () -> platoService.crear(plato2));
    }

    @Test
    void obtenerPorId_NoExiste_LanzaExcepcion() {
        assertThrows(RecursoNoEncontradoException.class, () -> platoService.obtenerPorId(99L));
    }

    @Test
    void obtenerTodos_DevuelveLista() {
        Plato plato = Plato.builder().nombre("Jugos").precio(5000.0).categoria("Bebida").disponible(true).build();
        platoService.crear(plato);

        List<Plato> platos = platoService.obtenerTodos();
        assertFalse(platos.isEmpty());
    }

    @Test
    void actualizarPlato_Exito() {
        Plato plato = platoService.crear(Plato.builder().nombre("Arroz").precio(8000.0).categoria("Acompañamiento").disponible(true).build());

        Long id = plato.getId();
        Plato actualizacion = Plato.builder().nombre("Arroz con Coco").precio(10000.0).categoria("Acompañamiento").disponible(true).build();
        Plato actualizado = platoService.actualizar(id, actualizacion);

        assertEquals("Arroz con Coco", actualizado.getNombre());
        assertEquals(10000.0, actualizado.getPrecio());
    }

    @Test
    void actualizarPlato_ConflictoNombreOtroPlato() {
        platoService.crear(Plato.builder().nombre("Carne").precio(30000.0).categoria("Fuerte").disponible(true).build());
        Plato p2 = platoService.crear(Plato.builder().nombre("Pollo").precio(25000.0).categoria("Fuerte").disponible(true).build());

        Long idP2 = p2.getId();
        Plato actualizacionInvalida = Plato.builder().nombre("Carne").precio(25000.0).categoria("Fuerte").disponible(true).build();
        assertThrows(ConflictoException.class, () -> platoService.actualizar(idP2, actualizacionInvalida));
    }

    @Test
    void eliminarPlato_Exito() {
        Plato plato = platoService.crear(Plato.builder().nombre("Ensalada").precio(15000.0).categoria("Entrada").disponible(true).build());
        Long id = plato.getId();
        platoService.eliminar(id);

        assertThrows(RecursoNoEncontradoException.class, () -> platoService.obtenerPorId(id));
    }
}