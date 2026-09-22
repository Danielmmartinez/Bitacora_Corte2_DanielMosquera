package edu.dosw.restaurante.service;

import edu.dosw.restaurante.domain.EstadoMesa;
import edu.dosw.restaurante.domain.Mesa;
import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.service.IMesaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class MesaServiceImpl implements IMesaService {

    private final Map<Long, Mesa> repositorio = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public List<Mesa> obtenerTodas() {
        log.info("Consultando todas las mesas en memoria");
        return repositorio.values().stream().toList();
    }

    @Override
    public Mesa obtenerPorId(Long id) {
        log.info("Buscando mesa con ID: {}", id);
        return repositorio.values().stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la mesa con ID: " + id));
    }

    @Override
    public Mesa crear(Mesa mesa) {
        log.info("Registrando nueva mesa número: {}", mesa.getNumero());

        boolean existeNumero = repositorio.values().stream()
                .anyMatch(m -> m.getNumero().equals(mesa.getNumero()));

        if (existeNumero) {
            throw new ConflictoException("Ya existe una mesa registrada con el número: " + mesa.getNumero());
        }

        Long nuevoId = idSequence.getAndIncrement();
        mesa.setId(nuevoId);
        if (mesa.getEstado() == null) {
            mesa.setEstado(EstadoMesa.DISPONIBLE);
        }
        if (mesa.getCuentaAbierta() == null) {
            mesa.setCuentaAbierta(false);
        }

        repositorio.put(nuevoId, mesa);
        return mesa;
    }

    @Override
    public Mesa cambiarEstado(Long id, EstadoMesa nuevoEstado) {
        log.info("Cambiando estado de la mesa ID: {} a {}", id, nuevoEstado);
        Mesa mesa = obtenerPorId(id);

        if (EstadoMesa.OCUPADA.equals(nuevoEstado)) {
            mesa.abrirCuenta();
        } else if (EstadoMesa.DISPONIBLE.equals(nuevoEstado)) {
            mesa.cerrarCuenta();
        } else {
            mesa.setEstado(nuevoEstado);
        }

        repositorio.put(id, mesa);
        return mesa;
    }

    @Override
    public void eliminar(Long id) {
        log.info("Eliminando mesa con ID: {}", id);
        obtenerPorId(id); // Valida existencia
        repositorio.remove(id);
    }
}