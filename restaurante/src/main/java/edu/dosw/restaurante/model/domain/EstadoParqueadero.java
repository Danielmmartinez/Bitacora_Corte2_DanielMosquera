package edu.dosw.restaurante.model.domain;

import java.util.List;

/**
 * Foto del parqueadero en este momento.
 */
public record EstadoParqueadero(int capacidad, long ocupados, long disponibles, List<RegistroVehiculo> activos) {
}
