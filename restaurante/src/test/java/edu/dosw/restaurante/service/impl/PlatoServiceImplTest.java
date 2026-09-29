package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.mapper.PlatoEntityMapper;
import edu.dosw.restaurante.mapper.PlatoEntityMapperImpl;
import edu.dosw.restaurante.model.domain.Plato;
import edu.dosw.restaurante.persistence.entity.PlatoEntity;
import edu.dosw.restaurante.repository.PlatoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Prueba unitaria: el repositorio es un mock (no hay BD). El mapper es el real generado por MapStruct.
 */
@ExtendWith(MockitoExtension.class)
class PlatoServiceImplTest {

    @Mock
    private PlatoRepository platoRepository;

    @Spy
    private PlatoEntityMapper entityMapper = new PlatoEntityMapperImpl();

    @InjectMocks
    private PlatoServiceImpl platoService;

    private PlatoEntity entidad(Long id, String nombre, String categoria, boolean disponible) {
        return PlatoEntity.builder().id(id).nombre(nombre).precio(20000.0).categoria(categoria).disponible(disponible).build();
    }

    private Plato plato(String nombre, String categoria, boolean disponible) {
        return Plato.builder().nombre(nombre).precio(20000.0).categoria(categoria).disponible(disponible).build();
    }

    // id que traía la entidad en el momento de llamar a save (antes de que la "BD" le asigne uno)
    private final List<Long> idsRecibidosEnSave = new ArrayList<>();

    // Simula a la BD: si la entidad no tiene id, le asigna uno
    private void saveAsignaId(Long id) {
        when(platoRepository.save(any())).thenAnswer(inv -> {
            PlatoEntity e = inv.getArgument(0);
            idsRecibidosEnSave.add(e.getId());
            if (e.getId() == null) e.setId(id);
            return e;
        });
    }

    @Test
    void crear_Exito_GuardaSinIdYDevuelveElAsignadoPorLaBD() {
        when(platoRepository.existsByNombreIgnoreCase("Pasta")).thenReturn(false);
        saveAsignaId(7L);

        Plato creado = platoService.crear(plato("Pasta", "Fuerte", true));

        assertEquals(7L, creado.getId());
        assertEquals("Pasta", creado.getNombre());
        assertNull(idsRecibidosEnSave.get(0)); // se envió sin id => INSERT
    }

    @Test
    void crear_IgnoraIdQueVengaEnElDominio() {
        when(platoRepository.existsByNombreIgnoreCase(any())).thenReturn(false);
        saveAsignaId(1L);
        Plato conId = plato("Ramen", "Sopas", true);
        conId.setId(99L);

        platoService.crear(conId);

        assertNull(idsRecibidosEnSave.get(0));
    }

    @Test
    void crear_NombreDuplicado_LanzaConflictoYNoGuarda() {
        when(platoRepository.existsByNombreIgnoreCase("sopa")).thenReturn(true);

        Plato duplicado = plato("sopa", "Entrada", true);
        assertThrows(ConflictoException.class, () -> platoService.crear(duplicado));
        verify(platoRepository, never()).save(any());
    }

    @Test
    void obtenerPorId_Existe_DevuelveDominio() {
        when(platoRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, "Gyoza", "Entradas", true)));

        Plato plato = platoService.obtenerPorId(1L);

        assertEquals(1L, plato.getId());
        assertEquals("Gyoza", plato.getNombre());
    }

    @Test
    void obtenerPorId_NoExiste_LanzaExcepcion() {
        when(platoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> platoService.obtenerPorId(99L));
    }

    @Test
    void obtenerTodos_ConvierteEntidadesADominio() {
        when(platoRepository.findAll()).thenReturn(List.of(
                entidad(1L, "Nigiri", "Sushi", true), entidad(2L, "Sashimi", "Sushi", false)));

        List<Plato> platos = platoService.obtenerTodos();

        assertEquals(2, platos.size());
        assertEquals("Sashimi", platos.get(1).getNombre());
    }

    @Test
    @DisplayName("obtenerTodos - sin platos devuelve lista vacía, no null")
    void obtenerTodos_SinPlatos_DevuelveListaVacia() {
        when(platoRepository.findAll()).thenReturn(List.of());
        List<Plato> platos = platoService.obtenerTodos();
        assertNotNull(platos);
        assertTrue(platos.isEmpty());
    }

    @Test
    void obtenerDisponibles_DelegaElFiltroALaBD() {
        when(platoRepository.findByDisponibleTrue()).thenReturn(List.of(entidad(1L, "Nigiri", "Sushi", true)));

        assertEquals(1, platoService.obtenerDisponibles().size());
        verify(platoRepository, never()).findAll();
    }

    @Test
    void obtenerDisponiblesPorCategoria_DelegaElFiltroALaBD() {
        when(platoRepository.findByDisponibleTrueAndCategoriaIgnoreCase("rolls"))
                .thenReturn(List.of(entidad(1L, "California Roll", "Rolls", true)));

        List<Plato> rolls = platoService.obtenerDisponiblesPorCategoria("rolls");

        assertEquals(1, rolls.size());
        assertEquals("California Roll", rolls.get(0).getNombre());
    }

    @Test
    void obtenerDisponiblePorId_Disponible_DevuelvePlato() {
        when(platoRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, "Gyoza", "Entradas", true)));
        assertEquals("Gyoza", platoService.obtenerDisponiblePorId(1L).getNombre());
    }

    @Test
    void obtenerDisponiblePorId_Agotado_LanzaNoEncontrado() {
        when(platoRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, "Tempura", "Entradas", false)));
        assertThrows(RecursoNoEncontradoException.class, () -> platoService.obtenerDisponiblePorId(1L));
    }

    @Test
    void actualizar_Exito_GuardaConElMismoId() {
        when(platoRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, "Arroz", "Acompañamiento", true)));
        when(platoRepository.existsByNombreIgnoreCaseAndIdNot("Arroz con Coco", 1L)).thenReturn(false);
        saveAsignaId(null);

        Plato cambios = Plato.builder().nombre("Arroz con Coco").precio(10000.0).categoria("Acompañamiento").disponible(false).build();
        Plato actualizado = platoService.actualizar(1L, cambios);

        assertEquals("Arroz con Coco", actualizado.getNombre());
        assertEquals(10000.0, actualizado.getPrecio());
        assertFalse(actualizado.estaDisponible());
        ArgumentCaptor<PlatoEntity> captor = ArgumentCaptor.forClass(PlatoEntity.class);
        verify(platoRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getId()); // con id => UPDATE, no INSERT
    }

    @Test
    void actualizar_NombreDeOtroPlato_LanzaConflicto() {
        when(platoRepository.findById(2L)).thenReturn(Optional.of(entidad(2L, "Pollo", "Fuerte", true)));
        when(platoRepository.existsByNombreIgnoreCaseAndIdNot("Carne", 2L)).thenReturn(true);

        Plato cambios = plato("Carne", "Fuerte", true);
        assertThrows(ConflictoException.class, () -> platoService.actualizar(2L, cambios));
        verify(platoRepository, never()).save(any());
    }

    @Test
    void actualizar_NoExiste_LanzaExcepcion() {
        when(platoRepository.findById(99L)).thenReturn(Optional.empty());

        Plato datos = plato("Fantasma", "X", true);
        assertThrows(RecursoNoEncontradoException.class, () -> platoService.actualizar(99L, datos));
        verify(platoRepository, never()).save(any());
    }

    @Test
    void cambiarDisponibilidad_Desactiva() {
        when(platoRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, "Edamame", "Entradas", true)));
        saveAsignaId(null);

        assertFalse(platoService.cambiarDisponibilidad(1L, false).estaDisponible());

        ArgumentCaptor<PlatoEntity> captor = ArgumentCaptor.forClass(PlatoEntity.class);
        verify(platoRepository).save(captor.capture());
        assertFalse(captor.getValue().getDisponible());
    }

    @Test
    void cambiarDisponibilidad_Activa() {
        when(platoRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, "Edamame", "Entradas", false)));
        saveAsignaId(null);

        assertTrue(platoService.cambiarDisponibilidad(1L, true).estaDisponible());
    }

    @Test
    void cambiarDisponibilidad_NoExiste_LanzaExcepcion() {
        when(platoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> platoService.cambiarDisponibilidad(99L, true));
    }

    @Test
    void eliminar_Exito_BorraEnLaBD() {
        when(platoRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, "Ensalada", "Entrada", true)));

        platoService.eliminar(1L);

        verify(platoRepository).deleteById(1L);
    }

    @Test
    void eliminar_NoExiste_LanzaExcepcionYNoBorra() {
        when(platoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> platoService.eliminar(99L));
        verify(platoRepository, never()).deleteById(any());
    }
}
