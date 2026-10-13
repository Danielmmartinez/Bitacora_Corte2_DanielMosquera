package edu.dosw.restaurante.service;

import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.model.domain.Reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * emailPropietario: si no es null, la operación solo aplica a reservas de ese cliente
 * (las ajenas se tratan como inexistentes). El personal pasa null y ve todas.
 */
public interface IReservaService {
    List<Reserva> obtenerTodas(LocalDate fecha);
    List<Reserva> obtenerDeCliente(String email);
    List<Reserva> obtenerEntre(LocalDateTime desde, LocalDateTime hasta);
    Reserva obtenerPorId(Long id, String emailPropietario);
    List<Mesa> mesasDisponibles(LocalDateTime fechaHora, int comensales);
    Reserva crear(Reserva reserva);
    Reserva actualizar(Long id, Reserva datos, String emailPropietario);
    Reserva cancelar(Long id, String emailPropietario);
    Reserva registrarLlegada(Long id);
}
