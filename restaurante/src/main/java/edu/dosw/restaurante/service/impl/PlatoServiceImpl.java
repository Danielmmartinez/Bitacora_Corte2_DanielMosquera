package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.mapper.PlatoEntityMapper;
import edu.dosw.restaurante.model.domain.Plato;
import edu.dosw.restaurante.persistence.entity.PlatoEntity;
import edu.dosw.restaurante.repository.PlatoRepository;
import edu.dosw.restaurante.service.IPlatoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // por defecto, cada método es una transacción de solo lectura
public class PlatoServiceImpl implements IPlatoService {

    private final PlatoRepository platoRepository;
    private final PlatoEntityMapper entityMapper;

    @Override
    public List<Plato> obtenerTodos() {
        log.info("Consultando todos los platos");
        return platoRepository.findAll().stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Plato> obtenerDisponibles() {
        log.info("Consultando platos disponibles");
        return platoRepository.findByDisponibleTrue().stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Plato> obtenerDisponiblesPorCategoria(String categoria) {
        log.info("Consultando platos disponibles de la categoría: {}", categoria);
        return platoRepository.findByDisponibleTrueAndCategoriaIgnoreCase(categoria).stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public Plato obtenerPorId(Long id) {
        log.info("Buscando plato con ID: {}", id);
        return platoRepository.findById(id)
                .map(entityMapper::toDomain)
                .orElseThrow(() -> {
                    log.warn("Plato no encontrado: id={}", id);
                    return new RecursoNoEncontradoException("No se encontró el plato con ID: " + id);
                });
    }

    @Override
    public Plato obtenerDisponiblePorId(Long id) {
        Plato plato = obtenerPorId(id);
        if (!plato.estaDisponible()) {
            throw new RecursoNoEncontradoException("No se encontró un plato disponible con ID: " + id);
        }
        return plato;
    }

    @Override
    @Transactional
    public Plato crear(Plato plato) {
        log.info("Registrando nuevo plato: {}", plato.getNombre());

        if (platoRepository.existsByNombreIgnoreCase(plato.getNombre())) {
            log.warn("Intento de crear plato duplicado: {}", plato.getNombre());
            throw new ConflictoException("Ya existe un plato registrado con el nombre: " + plato.getNombre());
        }

        plato.setId(null); // id null => INSERT; la BD asigna el ID
        PlatoEntity guardado = platoRepository.save(entityMapper.toEntity(plato));
        log.info("Plato creado: id={}", guardado.getId());
        return entityMapper.toDomain(guardado);
    }

    @Override
    @Transactional
    public Plato actualizar(Long id, Plato platoActualizado) {
        log.info("Actualizando plato con ID: {}", id);
        Plato platoExistente = obtenerPorId(id);

        if (platoRepository.existsByNombreIgnoreCaseAndIdNot(platoActualizado.getNombre(), id)) {
            throw new ConflictoException("Ya existe otro plato registrado con el nombre: " + platoActualizado.getNombre());
        }

        platoExistente.setNombre(platoActualizado.getNombre());
        platoExistente.setPrecio(platoActualizado.getPrecio());
        platoExistente.setCategoria(platoActualizado.getCategoria());
        platoExistente.setDisponible(platoActualizado.getDisponible());

        // Con id => UPDATE
        return entityMapper.toDomain(platoRepository.save(entityMapper.toEntity(platoExistente)));
    }

    @Override
    @Transactional
    public Plato cambiarDisponibilidad(Long id, boolean disponible) {
        Plato plato = obtenerPorId(id);
        if (disponible) {
            plato.activar();
        } else {
            plato.desactivar();
        }
        log.info("Plato id={} -> disponible={}", id, disponible);
        return entityMapper.toDomain(platoRepository.save(entityMapper.toEntity(plato)));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando plato con ID: {}", id);
        obtenerPorId(id); // Valida que exista antes de borrar (404)
        platoRepository.deleteById(id);
    }
}
