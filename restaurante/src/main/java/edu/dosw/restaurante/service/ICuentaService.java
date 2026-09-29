package edu.dosw.restaurante.service;

import edu.dosw.restaurante.model.domain.Cuenta;
import edu.dosw.restaurante.model.domain.EstadoCuenta;
import edu.dosw.restaurante.model.domain.MetodoPago;

import java.time.LocalDateTime;
import java.util.List;

public interface ICuentaService {
    List<Cuenta> obtenerTodas();
    List<Cuenta> obtenerPorEstado(EstadoCuenta estado);
    List<Cuenta> obtenerCerradasEntre(LocalDateTime desde, LocalDateTime hasta);
    Cuenta obtenerPorId(Long id);
    Cuenta obtenerAbiertaPorMesa(Long idMesa);
    Cuenta abrir(Long idMesa);
    Cuenta solicitarPago(Long id);
    Cuenta pagar(Long id, MetodoPago metodoPago, Double montoRecibido);
}
