package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.mapper.MesaEntityMapper;
import edu.dosw.restaurante.mapper.MesaEntityMapperImpl;
import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.persistence.entity.MesaEntity;
import edu.dosw.restaurante.repository.MesaRepository;
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

@ExtendWith(MockitoExtension.class)
class MesaServiceImplTest {

    @Mock
    private MesaRepository mesaRepository;

    @Spy
    private MesaEntityMapper entityMapper = new MesaEntityMapperImpl();

    @InjectMocks
    private MesaServiceImpl mesaService;

    private MesaEntity entidad(Long id, int numero, EstadoMesa estado, boolean cuentaAbierta) {
        return MesaEntity.builder().id(id).numero(numero).capacidad(4).estado(estado).cuentaAbierta(cuentaAbierta).build();
    }

    private final List<Long> idsRecibidosEnSave = new ArrayList<>();

    private void saveDevuelveLoMismo() {
        when(mesaRepository.save(any())).thenAnswer(inv -> {
            MesaEntity e = inv.getArgument(0);
            idsRecibidosEnSave.add(e.getId());
            if (e.getId() == null) e.setId(1L);
            return e;
        });
    }

    private MesaEntity capturarGuardado() {
        ArgumentCaptor<MesaEntity> captor = ArgumentCaptor.forClass(MesaEntity.class);
        verify(mesaRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void crear_Exito_AsignaValoresPorDefecto() {
        when(mesaRepository.existsByNumero(1)).thenReturn(false);
        saveDevuelveLoMismo();

        Mesa creada = mesaService.crear(Mesa.builder().numero(1).capacidad(4).build());

        assertEquals(1L, creada.getId());
        assertEquals(EstadoMesa.DISPONIBLE, creada.getEstado());
        assertFalse(creada.getCuentaAbierta());
        assertNull(idsRecibidosEnSave.get(0));
    }

    @Test
    void crear_NumeroDuplicado_LanzaConflicto() {
        when(mesaRepository.existsByNumero(5)).thenReturn(true);

        Mesa duplicada = Mesa.builder().numero(5).capacidad(4).build();
        assertThrows(ConflictoException.class, () -> mesaService.crear(duplicada));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void obtenerPorId_NoExiste_LanzaExcepcion() {
        when(mesaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> mesaService.obtenerPorId(99L));
    }

    @Test
    void obtenerTodas_ConvierteEntidades() {
        when(mesaRepository.findAll()).thenReturn(List.of(entidad(1L, 10, EstadoMesa.DISPONIBLE, false)));

        List<Mesa> mesas = mesaService.obtenerTodas();

        assertEquals(1, mesas.size());
        assertEquals(10, mesas.get(0).getNumero());
    }

    @Test
    void obtenerTodas_SinMesas_DevuelveListaVacia() {
        when(mesaRepository.findAll()).thenReturn(List.of());
        assertTrue(mesaService.obtenerTodas().isEmpty());
    }

    @Test
    void obtenerPorEstado_DelegaAlRepositorio() {
        when(mesaRepository.findByEstado(EstadoMesa.OCUPADA)).thenReturn(List.of(entidad(2L, 2, EstadoMesa.OCUPADA, true)));

        List<Mesa> ocupadas = mesaService.obtenerPorEstado(EstadoMesa.OCUPADA);

        assertEquals(1, ocupadas.size());
        assertEquals(2L, ocupadas.get(0).getId());
    }

    @Test
    void cambiarEstado_ReservarSinCuenta_Exito() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, 1, EstadoMesa.DISPONIBLE, false)));
        saveDevuelveLoMismo();

        assertEquals(EstadoMesa.RESERVADA, mesaService.cambiarEstado(1L, EstadoMesa.RESERVADA).getEstado());
    }

    @Test
    void cambiarEstado_DeReservadaADisponible_Exito() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, 1, EstadoMesa.RESERVADA, false)));
        saveDevuelveLoMismo();

        assertEquals(EstadoMesa.DISPONIBLE, mesaService.cambiarEstado(1L, EstadoMesa.DISPONIBLE).getEstado());
    }

    @Test
    void cambiarEstado_AOcupada_LanzaEstadoInvalido_SeOcupaAbriendoCuenta() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, 1, EstadoMesa.DISPONIBLE, false)));

        assertThrows(EstadoInvalidoException.class, () -> mesaService.cambiarEstado(1L, EstadoMesa.OCUPADA));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_ConCuentaAbierta_LanzaEstadoInvalido() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, 7, EstadoMesa.OCUPADA, true)));

        assertThrows(EstadoInvalidoException.class, () -> mesaService.cambiarEstado(1L, EstadoMesa.DISPONIBLE));
        assertThrows(EstadoInvalidoException.class, () -> mesaService.cambiarEstado(1L, EstadoMesa.RESERVADA));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void ocupar_AbreCuentaEnLaMesa() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, 1, EstadoMesa.DISPONIBLE, false)));
        saveDevuelveLoMismo();

        Mesa ocupada = mesaService.ocupar(1L, 5L);

        assertEquals(EstadoMesa.OCUPADA, ocupada.getEstado());
        assertTrue(ocupada.getCuentaAbierta());
        assertEquals(5L, capturarGuardado().getIdCuentaAbierta());
    }

    @Test
    void liberar_CierraCuentaEnLaMesa() {
        MesaEntity ocupada = entidad(1L, 1, EstadoMesa.OCUPADA, true);
        ocupada.setIdCuentaAbierta(5L);
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(ocupada));
        saveDevuelveLoMismo();

        Mesa libre = mesaService.liberar(1L);

        assertEquals(EstadoMesa.DISPONIBLE, libre.getEstado());
        assertFalse(libre.getCuentaAbierta());
        assertNull(capturarGuardado().getIdCuentaAbierta());
    }

    @Test
    void cambiarEstado_NoExiste_LanzaExcepcion() {
        when(mesaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> mesaService.cambiarEstado(99L, EstadoMesa.OCUPADA));
    }

    @Test
    void actualizar_Exito_ConservaEstado() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(entidad(1L, 3, EstadoMesa.OCUPADA, true)));
        when(mesaRepository.existsByNumeroAndIdNot(30, 1L)).thenReturn(false);
        saveDevuelveLoMismo();

        Mesa actualizada = mesaService.actualizar(1L, Mesa.builder().numero(30).capacidad(6).build());

        assertEquals(30, actualizada.getNumero());
        assertEquals(6, actualizada.getCapacidad());
        assertEquals(EstadoMesa.OCUPADA, actualizada.getEstado());
        assertEquals(1L, capturarGuardado().getId());
    }

    @Test
    void actualizar_NumeroDeOtraMesa_LanzaConflicto() {
        when(mesaRepository.findById(2L)).thenReturn(Optional.of(entidad(2L, 2, EstadoMesa.DISPONIBLE, false)));
        when(mesaRepository.existsByNumeroAndIdNot(1, 2L)).thenReturn(true);

        Mesa conflicto = Mesa.builder().numero(1).capacidad(2).build();
        assertThrows(ConflictoException.class, () -> mesaService.actualizar(2L, conflicto));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void eliminar_Exito() {
        when(mesaRepository.findById(3L)).thenReturn(Optional.of(entidad(3L, 3, EstadoMesa.DISPONIBLE, false)));

        mesaService.eliminar(3L);

        verify(mesaRepository).deleteById(3L);
    }

    @Test
    void eliminar_ConCuentaAbierta_LanzaConflicto() {
        when(mesaRepository.findById(8L)).thenReturn(Optional.of(entidad(8L, 8, EstadoMesa.OCUPADA, true)));

        assertThrows(ConflictoException.class, () -> mesaService.eliminar(8L));
        verify(mesaRepository, never()).deleteById(any());
    }

    @Test
    void eliminar_NoExiste_LanzaExcepcion() {
        when(mesaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> mesaService.eliminar(99L));
    }
}
