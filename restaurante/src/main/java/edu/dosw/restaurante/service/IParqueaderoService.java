package edu.dosw.restaurante.service;

import edu.dosw.restaurante.model.domain.EstadoParqueadero;
import edu.dosw.restaurante.model.domain.RegistroVehiculo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IParqueaderoService {
    RegistroVehiculo registrarEntrada(String placa);
    RegistroVehiculo registrarSalida(String placa);
    EstadoParqueadero obtenerEstado();
    RegistroVehiculo obtenerPorId(Long id);
    List<RegistroVehiculo> obtenerRegistrosDelDia(LocalDate fecha);
    List<RegistroVehiculo> obtenerSalidasEntre(LocalDateTime desde, LocalDateTime hasta);
}
