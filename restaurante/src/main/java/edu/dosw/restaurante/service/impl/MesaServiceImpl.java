package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.mapper.MesaEntityMapper;
import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.repository.MesaRepository;
import edu.dosw.restaurante.service.IMesaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MesaServiceImpl implements IMesaService {

    private final MesaRepository mesaRepository;
    private final MesaEntityMapper entityMapper;

    @Override
    public List<Mesa> obtenerTodas() {
        log.info("Consultando todas las mesas");
        return mesaRepository.findAll().stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Mesa> obtenerPorEstado(EstadoMesa estado) {
        log.info("Consultando mesas en estado: {}", estado);
        return mesaRepository.findByEstado(estado).stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public Mesa obtenerPorId(Long id) {
        log.info("Buscando mesa con ID: {}", id);
        return mesaRepository.findById(id)
                .map(entityMapper::toDomain)
                .orElseThrow(() -> {
                    log.warn("Mesa no encontrada: id={}", id);
                    return new RecursoNoEncontradoException("No se encontró la mesa con ID: " + id);
                });
    }

    @Override
    @Transactional
    public Mesa crear(Mesa mesa) {
        log.info("Registrando nueva mesa número: {}", mesa.getNumero());

        if (mesaRepository.existsByNumero(mesa.getNumero())) {
            throw new ConflictoException("Ya existe una mesa registrada con el número: " + mesa.getNumero());
        }

        mesa.setId(null);
        if (mesa.getEstado() == null) {
            mesa.setEstado(EstadoMesa.DISPONIBLE);
        }
        if (mesa.getCuentaAbierta() == null) {
            mesa.setCuentaAbierta(false);
        }

        return guardar(mesa);
    }

    @Override
    @Transactional
    public Mesa actualizar(Long id, Mesa mesaActualizada) {
        log.info("Actualizando mesa con ID: {}", id);
        Mesa existente = obtenerPorId(id);

        if (mesaRepository.existsByNumeroAndIdNot(mesaActualizada.getNumero(), id)) {
            throw new ConflictoException("Ya existe una mesa registrada con el número: " + mesaActualizada.getNumero());
        }

        existente.setNumero(mesaActualizada.getNumero());
        existente.setCapacidad(mesaActualizada.getCapacidad());
        return guardar(existente);
    }

    /**
     * Cambio manual de estado: solo entre DISPONIBLE y RESERVADA.
     * OCUPADA se alcanza abriendo una cuenta (POST /cuentas) y se sale de ella pagando.
     */
    @Override
    @Transactional
    public Mesa cambiarEstado(Long id, EstadoMesa nuevoEstado) {
        log.info("Cambiando estado de la mesa ID: {} a {}", id, nuevoEstado);
        Mesa mesa = obtenerPorId(id);

        if (mesa.tieneCuentaAbierta()) {
            throw new EstadoInvalidoException("La mesa " + mesa.getNumero()
                    + " tiene la cuenta " + mesa.getIdCuentaAbierta() + " abierta; se libera al pagarla");
        }
        if (EstadoMesa.OCUPADA.equals(nuevoEstado)) {
            throw new EstadoInvalidoException("Para ocupar la mesa " + mesa.getNumero() + " abra una cuenta (POST /api/v1/cuentas)");
        }

        mesa.setEstado(nuevoEstado);
        return guardar(mesa);
    }

    @Override
    @Transactional
    public Mesa ocupar(Long id, Long idCuenta) {
        Mesa mesa = obtenerPorId(id);
        mesa.abrirCuenta(idCuenta);
        log.info("Mesa {} ocupada con la cuenta {}", mesa.getNumero(), idCuenta);
        return guardar(mesa);
    }

    @Override
    @Transactional
    public Mesa liberar(Long id) {
        Mesa mesa = obtenerPorId(id);
        mesa.cerrarCuenta();
        log.info("Mesa {} liberada", mesa.getNumero());
        return guardar(mesa);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando mesa con ID: {}", id);
        Mesa mesa = obtenerPorId(id);
        if (mesa.tieneCuentaAbierta()) {
            throw new ConflictoException("No se puede eliminar la mesa " + mesa.getNumero() + " porque tiene una cuenta abierta");
        }
        mesaRepository.deleteById(id);
    }

    private Mesa guardar(Mesa mesa) {
        return entityMapper.toDomain(mesaRepository.save(entityMapper.toEntity(mesa)));
    }
}
