package edu.dosw.restaurante.service;

import edu.dosw.restaurante.domain.Plato;
import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.service.IPlatoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class PlatoServiceImpl implements IPlatoService {

    private final Map<Long, Plato> repositorio = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public List<Plato> obtenerTodos() {
        log.info("Consultando todos los platos en memoria");
        return repositorio.values().stream().toList();
    }

    @Override
    public Plato obtenerPorId(Long id) {
        log.info("Buscando plato con ID: {}", id);
        return repositorio.values().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el plato con ID: " + id));
    }

    @Override
    public Plato crear(Plato plato) {
        log.info("Registrando nuevo plato: {}", plato.getNombre());

        boolean existeNombre = repositorio.values().stream()
                .anyMatch(p -> p.getNombre().equalsIgnoreCase(plato.getNombre()));

        if (existeNombre) {
            throw new ConflictoException("Ya existe un plato registrado con el nombre: " + plato.getNombre());
        }

        Long nuevoId = idSequence.getAndIncrement();
        plato.setId(nuevoId);
        repositorio.put(nuevoId, plato);

        return plato;
    }

    @Override
    public Plato actualizar(Long id, Plato platoActualizado) {
        log.info("Actualizando plato con ID: {}", id);
        Plato platoExistente = obtenerPorId(id);

        boolean existeOtroConMismoNombre = repositorio.values().stream()
                .anyMatch(p -> !p.getId().equals(id) && p.getNombre().equalsIgnoreCase(platoActualizado.getNombre()));

        if (existeOtroConMismoNombre) {
            throw new ConflictoException("Ya existe otro plato registrado con el nombre: " + platoActualizado.getNombre());
        }

        platoExistente.setNombre(platoActualizado.getNombre());
        platoExistente.setPrecio(platoActualizado.getPrecio());
        platoExistente.setCategoria(platoActualizado.getCategoria());
        platoExistente.setDisponible(platoActualizado.getDisponible());

        repositorio.put(id, platoExistente);
        return platoExistente;
    }

    @Override
    public void eliminar(Long id) {
        log.info("Eliminando plato con ID: {}", id);
        obtenerPorId(id); // Valida que exista antes de remover
        repositorio.remove(id);
    }
}