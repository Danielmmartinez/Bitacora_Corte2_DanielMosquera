package edu.dosw.restaurante.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Reserva de una mesa para una fecha y hora. Ocupa la mesa durante una duración fija
 * (configurable), así que dos reservas de la misma mesa no pueden cruzarse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {
    private Long id;
    private Long idMesa;
    private String nombreCliente;
    private String emailCliente;   // dueño de la reserva (para que un cliente solo vea las suyas)
    private LocalDateTime fechaHora;
    private Integer comensales;
    private EstadoReserva estado;
    private LocalDateTime fechaCreacion;
    private Long idCuenta;         // cuenta abierta al registrar la llegada

    public boolean estaConfirmada() {
        return EstadoReserva.CONFIRMADA.equals(estado);
    }

    public boolean estaVigente(LocalDateTime ahora) {
        return estaConfirmada() && fechaHora != null && fechaHora.isAfter(ahora);
    }

    public LocalDateTime fin(int duracionMinutos) {
        return fechaHora.plusMinutes(duracionMinutos);
    }

    /**
     * ¿Esta reserva (existente o todavía por crear) choca con "otra" ya registrada?
     * Solo bloquean las reservas CONFIRMADAS. Dos intervalos [inicio, fin) se cruzan si cada uno
     * empieza antes de que termine el otro: una reserva que empieza justo cuando termina la otra NO se cruza.
     */
    public boolean seSolapaCon(Reserva otra, int duracionMinutos) {
        return idMesa != null && idMesa.equals(otra.getIdMesa())
                && otra.estaConfirmada()
                && fechaHora.isBefore(otra.fin(duracionMinutos))
                && otra.getFechaHora().isBefore(fin(duracionMinutos));
    }

    public boolean perteneceA(String email) {
        return email != null && email.equalsIgnoreCase(emailCliente);
    }

    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
    }

    public void registrarLlegada() {
        this.estado = EstadoReserva.CUMPLIDA;
    }

    public void reprogramar(LocalDateTime nuevaFechaHora) {
        this.fechaHora = nuevaFechaHora;
    }
}
