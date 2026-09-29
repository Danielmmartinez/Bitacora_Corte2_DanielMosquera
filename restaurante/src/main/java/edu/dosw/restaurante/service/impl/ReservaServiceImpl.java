package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.config.ReservaProperties;
import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.exception.SolicitudInvalidaException;
import edu.dosw.restaurante.mapper.ReservaEntityMapper;
import edu.dosw.restaurante.model.domain.Cuenta;
import edu.dosw.restaurante.model.domain.EstadoReserva;
import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.model.domain.Reserva;
import edu.dosw.restaurante.repository.ReservaRepository;
import edu.dosw.restaurante.service.ICuentaService;
import edu.dosw.restaurante.service.IMesaService;
import edu.dosw.restaurante.service.IReservaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservaServiceImpl implements IReservaService {

    private final ReservaRepository reservaRepository;
    private final ReservaEntityMapper entityMapper;
    private final IMesaService mesaService;
    private final ICuentaService cuentaService;
    private final ReservaProperties propiedades;
    private final Clock clock;

    @Override
    public List<Reserva> obtenerTodas(LocalDate fecha) {
        if (fecha == null) {
            return reservaRepository.findAll(Sort.by("fechaHora")).stream().map(entityMapper::toDomain).toList();
        }
        return obtenerEntre(fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
    }

    @Override
    public List<Reserva> obtenerDeCliente(String email) {
        return reservaRepository.findByEmailClienteIgnoreCaseOrderByFechaHoraDesc(email).stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Reserva> obtenerEntre(LocalDateTime desde, LocalDateTime hasta) {
        return reservaRepository.findEntre(desde, hasta).stream().map(entityMapper::toDomain).toList();
    }

    @Override
    public Reserva obtenerPorId(Long id, String emailPropietario) {
        return reservaRepository.findById(id)
                .map(entityMapper::toDomain)
                // Una reserva ajena se responde como inexistente (404): no se revela que existe
                .filter(r -> emailPropietario == null || r.perteneceA(emailPropietario))
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la reserva con ID: " + id));
    }

    @Override
    public List<Mesa> mesasDisponibles(LocalDateTime fechaHora, int comensales) {
        if (comensales < 1) {
            throw new SolicitudInvalidaException("El número de comensales debe ser al menos 1");
        }
        validarHorario(fechaHora);

        return mesaService.obtenerTodas().stream()
                .filter(m -> m.getCapacidad() >= comensales)
                .filter(m -> buscarCruce(Reserva.builder().idMesa(m.getId()).fechaHora(fechaHora)
                        .estado(EstadoReserva.CONFIRMADA).build(), null).isEmpty())
                .sorted(Comparator.comparing(Mesa::getCapacidad)) // la más ajustada primero
                .toList();
    }

    @Override
    @Transactional
    public Reserva crear(Reserva reserva) {
        log.info("Creando reserva para la mesa {} el {}", reserva.getIdMesa(), reserva.getFechaHora());
        validarReglas(reserva, null);

        reserva.setId(null);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setFechaCreacion(LocalDateTime.now(clock));
        return guardar(reserva);
    }

    @Override
    @Transactional
    public Reserva actualizar(Long id, Reserva datos, String emailPropietario) {
        Reserva existente = obtenerPorId(id, emailPropietario);
        validarModificable(existente);

        existente.setIdMesa(datos.getIdMesa());
        existente.setNombreCliente(datos.getNombreCliente());
        existente.setComensales(datos.getComensales());
        existente.reprogramar(datos.getFechaHora());
        if (emailPropietario == null && datos.getEmailCliente() != null) {
            existente.setEmailCliente(datos.getEmailCliente());
        }

        validarReglas(existente, id);
        return guardar(existente);
    }

    @Override
    @Transactional
    public Reserva cancelar(Long id, String emailPropietario) {
        Reserva reserva = obtenerPorId(id, emailPropietario);
        validarModificable(reserva);
        reserva.cancelar();
        log.info("Reserva {} cancelada", id);
        return guardar(reserva);
    }

    /**
     * El cliente llegó: la reserva se cumple y se abre la cuenta de la mesa.
     * Si falla la apertura (ej. la mesa sigue ocupada), la transacción deshace todo.
     */
    @Override
    @Transactional
    public Reserva registrarLlegada(Long id) {
        Reserva reserva = obtenerPorId(id, null);
        if (!reserva.estaConfirmada()) {
            throw new EstadoInvalidoException("La reserva " + id + " está " + reserva.getEstado());
        }
        LocalDate hoy = LocalDate.now(clock);
        if (!reserva.getFechaHora().toLocalDate().equals(hoy)) {
            throw new EstadoInvalidoException("La reserva " + id + " es para el " + reserva.getFechaHora().toLocalDate()
                    + ", no para hoy");
        }

        Cuenta cuenta = cuentaService.abrir(reserva.getIdMesa());
        reserva.registrarLlegada();
        reserva.setIdCuenta(cuenta.getId());
        log.info("Llegada de la reserva {}: cuenta {} abierta en la mesa {}", id, cuenta.getId(), reserva.getIdMesa());
        return guardar(reserva);
    }

    // ── reglas ─────────────────────────────────────────────────

    private void validarReglas(Reserva reserva, Long idExcluido) {
        Mesa mesa = mesaService.obtenerPorId(reserva.getIdMesa()); // 404 si no existe

        if (reserva.getComensales() > mesa.getCapacidad()) {
            throw new EstadoInvalidoException("La mesa " + mesa.getNumero() + " es para " + mesa.getCapacidad()
                    + " personas y la reserva es para " + reserva.getComensales());
        }
        if (!reserva.getFechaHora().isAfter(LocalDateTime.now(clock))) {
            throw new EstadoInvalidoException("La reserva debe ser para una fecha y hora futura");
        }
        validarHorario(reserva.getFechaHora());

        buscarCruce(reserva, idExcluido).ifPresent(otra -> {
            throw new ConflictoException("La mesa " + mesa.getNumero() + " ya está reservada a las "
                    + otra.getFechaHora().toLocalTime() + " (cada reserva ocupa la mesa "
                    + propiedades.duracionMinutos() + " minutos)");
        });
    }

    private void validarHorario(LocalDateTime fechaHora) {
        LocalTime hora = fechaHora.toLocalTime();
        if (hora.isBefore(propiedades.horaApertura()) || hora.isAfter(propiedades.horaUltimaReserva())) {
            throw new EstadoInvalidoException("Se reciben reservas entre las " + propiedades.horaApertura()
                    + " y las " + propiedades.horaUltimaReserva());
        }
    }

    private void validarModificable(Reserva reserva) {
        if (!reserva.estaVigente(LocalDateTime.now(clock))) {
            throw new EstadoInvalidoException("La reserva " + reserva.getId() + " ya no se puede modificar (estado "
                    + reserva.getEstado() + ")");
        }
    }

    // La BD trae las candidatas cercanas; el dominio decide si de verdad se cruzan
    private Optional<Reserva> buscarCruce(Reserva reserva, Long idExcluido) {
        int duracion = propiedades.duracionMinutos();
        return reservaRepository.findByIdMesaAndEstadoAndFechaHoraAfterAndFechaHoraBefore(
                        reserva.getIdMesa(), EstadoReserva.CONFIRMADA,
                        reserva.getFechaHora().minusMinutes(duracion), reserva.getFechaHora().plusMinutes(duracion))
                .stream()
                .map(entityMapper::toDomain)
                .filter(otra -> !otra.getId().equals(idExcluido))
                .filter(otra -> reserva.seSolapaCon(otra, duracion))
                .findFirst();
    }

    private Reserva guardar(Reserva reserva) {
        return entityMapper.toDomain(reservaRepository.save(entityMapper.toEntity(reserva)));
    }
}
