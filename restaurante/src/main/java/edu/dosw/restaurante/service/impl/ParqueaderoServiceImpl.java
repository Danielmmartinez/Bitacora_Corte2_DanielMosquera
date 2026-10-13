package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.config.ParqueaderoProperties;
import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.exception.SolicitudInvalidaException;
import edu.dosw.restaurante.mapper.RegistroVehiculoEntityMapper;
import edu.dosw.restaurante.model.domain.EstadoParqueadero;
import edu.dosw.restaurante.model.domain.RegistroVehiculo;
import edu.dosw.restaurante.model.domain.TipoVehiculo;
import edu.dosw.restaurante.repository.RegistroVehiculoRepository;
import edu.dosw.restaurante.service.IParqueaderoService;
import edu.dosw.restaurante.util.PlacaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParqueaderoServiceImpl implements IParqueaderoService {

    private final RegistroVehiculoRepository registroRepository;
    private final RegistroVehiculoEntityMapper entityMapper;
    private final ParqueaderoProperties propiedades;
    private final Clock clock;

    @Override
    @Transactional
    public RegistroVehiculo registrarEntrada(String placaRecibida) {
        String placa = PlacaUtils.normalizar(placaRecibida);
        TipoVehiculo tipo = TipoVehiculo.desdePlaca(placa)
                .orElseThrow(() -> new SolicitudInvalidaException("Placa inválida: " + placaRecibida
                        + ". Formatos: ABC123 (carro) o ABC12D (moto)"));

        if (registroRepository.existsByPlacaAndSalidaIsNull(placa)) {
            throw new ConflictoException("El vehículo " + placa + " ya está dentro del parqueadero");
        }
        long ocupados = registroRepository.countBySalidaIsNull();
        if (ocupados >= propiedades.capacidad()) {
            throw new EstadoInvalidoException("Parqueadero lleno: " + ocupados + " de " + propiedades.capacidad() + " cupos ocupados");
        }

        RegistroVehiculo registro = RegistroVehiculo.builder()
                .placa(placa)
                .tipo(tipo)
                .entrada(LocalDateTime.now(clock))
                .build();
        log.info("Entrada de {} ({}), cupos ocupados: {}", placa, tipo, ocupados + 1);
        return guardar(registro);
    }

    @Override
    @Transactional
    public RegistroVehiculo registrarSalida(String placaRecibida) {
        String placa = PlacaUtils.normalizar(placaRecibida);
        RegistroVehiculo registro = registroRepository.findByPlacaAndSalidaIsNull(placa)
                .map(entityMapper::toDomain)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay ningún vehículo con placa " + placa + " dentro del parqueadero"));

        registro.registrarSalida(LocalDateTime.now(clock), propiedades.tarifaPara(registro.getTipo()));
        log.info("Salida de {}: {} hora(s), cobro {}", placa, registro.horasACobrar(registro.getSalida()), registro.getCobro());
        return guardar(registro);
    }

    @Override
    public EstadoParqueadero obtenerEstado() {
        List<RegistroVehiculo> activos = registroRepository.findBySalidaIsNullOrderByEntradaAsc().stream()
                .map(entityMapper::toDomain)
                .toList();
        int capacidad = propiedades.capacidad();
        return new EstadoParqueadero(capacidad, activos.size(), Math.max(0, capacidad - activos.size()), activos);
    }

    @Override
    public RegistroVehiculo obtenerPorId(Long id) {
        return registroRepository.findById(id)
                .map(entityMapper::toDomain)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el registro con ID: " + id));
    }

    @Override
    public List<RegistroVehiculo> obtenerRegistrosDelDia(LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
        return registroRepository.findEntradasEntre(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay()).stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public List<RegistroVehiculo> obtenerSalidasEntre(LocalDateTime desde, LocalDateTime hasta) {
        return registroRepository.findSalidasEntre(desde, hasta).stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    private RegistroVehiculo guardar(RegistroVehiculo registro) {
        return entityMapper.toDomain(registroRepository.save(entityMapper.toEntity(registro)));
    }
}
