package edu.dosw.restaurante.service.impl;

import edu.dosw.restaurante.exception.ConflictoException;
import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.exception.RecursoNoEncontradoException;
import edu.dosw.restaurante.mapper.CuentaEntityMapper;
import edu.dosw.restaurante.model.domain.Cuenta;
import edu.dosw.restaurante.model.domain.EstadoCuenta;
import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.model.domain.MetodoPago;
import edu.dosw.restaurante.model.domain.Pedido;
import edu.dosw.restaurante.persistence.entity.CuentaEntity;
import edu.dosw.restaurante.repository.CuentaRepository;
import edu.dosw.restaurante.service.ICuentaService;
import edu.dosw.restaurante.service.IMesaService;
import edu.dosw.restaurante.service.IPedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuentaServiceImpl implements ICuentaService {

    private final CuentaRepository cuentaRepository;
    private final CuentaEntityMapper entityMapper;
    private final IMesaService mesaService;
    private final IPedidoService pedidoService;
    private final Clock clock;

    @Override
    public List<Cuenta> obtenerTodas() {
        return cuentaRepository.findAll().stream()
                .map(this::toDomainConPedidos)
                .toList();
    }

    @Override
    public List<Cuenta> obtenerPorEstado(EstadoCuenta estado) {
        return cuentaRepository.findByEstado(estado).stream()
                .map(this::toDomainConPedidos)
                .toList();
    }

    // Sin pedidos: los reportes solo necesitan el total congelado y el método de pago
    @Override
    public List<Cuenta> obtenerCerradasEntre(LocalDateTime desde, LocalDateTime hasta) {
        return cuentaRepository.findCerradasEntre(desde, hasta).stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Override
    public Cuenta obtenerPorId(Long id) {
        return cuentaRepository.findById(id)
                .map(this::toDomainConPedidos)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la cuenta con ID: " + id));
    }

    @Override
    public Cuenta obtenerAbiertaPorMesa(Long idMesa) {
        Mesa mesa = mesaService.obtenerPorId(idMesa);
        if (!mesa.tieneCuentaAbierta()) {
            throw new RecursoNoEncontradoException("La mesa " + mesa.getNumero() + " no tiene una cuenta abierta");
        }
        return obtenerPorId(mesa.getIdCuentaAbierta());
    }

    @Override
    @Transactional
    public Cuenta abrir(Long idMesa) {
        Mesa mesa = mesaService.obtenerPorId(idMesa);
        if (mesa.tieneCuentaAbierta()) {
            throw new ConflictoException("La mesa " + mesa.getNumero()
                    + " ya tiene abierta la cuenta " + mesa.getIdCuentaAbierta());
        }

        Cuenta cuenta = Cuenta.builder()
                .idMesa(idMesa)
                .estado(EstadoCuenta.ABIERTA)
                .fechaApertura(LocalDateTime.now(clock))
                .build();
        CuentaEntity guardada = cuentaRepository.save(entityMapper.toEntity(cuenta));

        mesaService.ocupar(idMesa, guardada.getId());
        log.info("Cuenta {} abierta para la mesa {}", guardada.getId(), mesa.getNumero());
        return toDomainConPedidos(guardada);
    }

    @Override
    @Transactional
    public Cuenta solicitarPago(Long id) {
        Cuenta cuenta = obtenerPorId(id);
        if (cuenta.getEstado() != EstadoCuenta.ABIERTA) {
            throw new EstadoInvalidoException("La cuenta " + id + " está en estado " + cuenta.getEstado()
                    + "; solo se puede solicitar una cuenta ABIERTA");
        }
        cuenta.solicitarPago();
        return guardar(cuenta);
    }

    @Override
    @Transactional
    public Cuenta pagar(Long id, MetodoPago metodoPago, Double montoRecibido) {
        Cuenta cuenta = obtenerPorId(id);

        if (cuenta.estaCerrada()) {
            throw new EstadoInvalidoException("La cuenta " + id + " ya fue pagada");
        }

        List<Pedido> pendientes = cuenta.pedidosPendientes();
        if (!pendientes.isEmpty()) {
            String detalle = pendientes.stream()
                    .map(p -> "#" + p.getId() + " (" + p.getEstado() + ")")
                    .collect(Collectors.joining(", "));
            throw new EstadoInvalidoException("No se puede pagar: hay pedidos sin entregar ni cancelar: " + detalle);
        }

        Double total = cuenta.calcularTotal();
        if (montoRecibido < total) {
            throw new EstadoInvalidoException("Monto insuficiente: el total es " + total + " y se recibió " + montoRecibido);
        }

        cuenta.pagar(metodoPago, montoRecibido, LocalDateTime.now(clock));
        Cuenta pagada = guardar(cuenta);
        mesaService.liberar(cuenta.getIdMesa());
        log.info("Cuenta {} pagada: total={}, método={}", id, total, metodoPago);
        return pagada;
    }

    private Cuenta guardar(Cuenta cuenta) {
        List<Pedido> pedidos = cuenta.getPedidos();
        Cuenta guardada = entityMapper.toDomain(cuentaRepository.save(entityMapper.toEntity(cuenta)));
        guardada.setPedidos(pedidos);
        return guardada;
    }

    private Cuenta toDomainConPedidos(CuentaEntity entity) {
        Cuenta cuenta = entityMapper.toDomain(entity);
        cuenta.setPedidos(pedidoService.obtenerPorCuenta(entity.getId()));
        return cuenta;
    }
}
