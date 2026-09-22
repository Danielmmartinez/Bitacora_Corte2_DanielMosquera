package edu.dosw.restaurante.service;

import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.model.domain.Mesa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MesaServiceImplTest {

    private MesaServiceImpl mesaService;

    @BeforeEach
    void setUp() {
        mesaService = new MesaServiceImpl();
    }

    @Test
    void crearYObtenerMesa_Exito() {
        Mesa mesa = Mesa.builder().numero(1).capacidad(4).build();
        Mesa creada = mesaService.crear(mesa);

        assertNotNull(creada.getId());
        assertEquals(EstadoMesa.DISPONIBLE, creada.getEstado());
        assertFalse(creada.getCuentaAbierta());
    }

    @Test
    void crearMesa_NumeroDuplicado_LanzaConflicto() {
        mesaService.crear(Mesa.builder().numero(5).capacidad(2).build());
        Mesa mesaDuplicada = Mesa.builder().numero(5).capacidad(4).build();
        assertThrows(ConflictoException.class, () -> mesaService.crear(mesaDuplicada));
    }

    @Test
    void cambiarEstado_OcupadaYDisponible() {
        Mesa creada = mesaService.crear(Mesa.builder().numero(2).capacidad(4).build());
        Long id = creada.getId();

        Mesa ocupada = mesaService.cambiarEstado(id, EstadoMesa.OCUPADA);
        assertEquals(EstadoMesa.OCUPADA, ocupada.getEstado());
        assertTrue(ocupada.getCuentaAbierta());

        Mesa disponible = mesaService.cambiarEstado(id, EstadoMesa.DISPONIBLE);
        assertEquals(EstadoMesa.DISPONIBLE, disponible.getEstado());
        assertFalse(disponible.getCuentaAbierta());

        Mesa reservada = mesaService.cambiarEstado(id, EstadoMesa.RESERVADA);
        assertEquals(EstadoMesa.RESERVADA, reservada.getEstado());
    }

    @Test
    void obtenerPorId_NoExiste_LanzaExcepcion() {
        assertThrows(RecursoNoEncontradoException.class, () -> mesaService.obtenerPorId(99L));
    }

    @Test
    void obtenerTodas_DevuelveLista() {
        mesaService.crear(Mesa.builder().numero(10).capacidad(2).build());
        List<Mesa> mesas = mesaService.obtenerTodas();
        assertFalse(mesas.isEmpty());
    }

    @Test
    void eliminarMesa_Exito() {
        Mesa creada = mesaService.crear(Mesa.builder().numero(3).capacidad(2).build());
        Long id = creada.getId();
        mesaService.eliminar(id);

        assertThrows(RecursoNoEncontradoException.class, () -> mesaService.obtenerPorId(id));
    }
}