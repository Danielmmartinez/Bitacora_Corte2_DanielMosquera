package edu.dosw.restaurante.model.dto.response;

import java.util.List;

public record EstadoParqueaderoResponseDTO(int capacidad, long ocupados, long disponibles,
                                           List<RegistroVehiculoResponseDTO> activos) {
}
