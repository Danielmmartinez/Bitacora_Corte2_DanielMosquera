package edu.dosw.restaurante.controller.docs;

import edu.dosw.restaurante.model.dto.request.ReservaRequestDTO;
import edu.dosw.restaurante.model.dto.response.MesaResponseDTO;
import edu.dosw.restaurante.model.dto.response.ReservaResponseDTO;
import edu.dosw.restaurante.model.dto.response.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Reservas", description = "Reservas de mesa. Un CLIENTE solo ve y modifica las suyas; el personal ve todas.")
public interface ReservaApi {

    @Operation(summary = "Listar reservas",
            description = "Personal: todas (filtro opcional por fecha). Cliente: solo las suyas.")
    @ApiResponse(responseCode = "200", description = "Reservas ordenadas por fecha")
    ResponseEntity<List<ReservaResponseDTO>> obtener(@Parameter(description = "Fecha (AAAA-MM-DD), opcional") LocalDate fecha,
                                                     @Parameter(hidden = true) Authentication auth);

    @Operation(summary = "Obtener una reserva")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe (o es de otro cliente)", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<ReservaResponseDTO> obtenerPorId(@Parameter(description = "ID de la reserva") Long id,
                                                    @Parameter(hidden = true) Authentication auth);

    @Operation(summary = "Mesas disponibles",
            description = "Mesas con capacidad suficiente y sin reservas que se crucen con esa fecha y hora")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mesas libres, de la más pequeña a la más grande"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Fuera del horario de reservas", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<List<MesaResponseDTO>> disponibilidad(
            @Parameter(description = "Fecha y hora, ej. 2026-10-01T19:30:00") LocalDateTime fechaHora,
            @Parameter(description = "Número de personas") int comensales);

    @Operation(summary = "Crear una reserva", description = "Si la crea un CLIENTE, queda a su nombre (email del token)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva CONFIRMADA"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o fecha pasada", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "La mesa no existe", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "La mesa ya está reservada en ese horario", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Excede la capacidad de la mesa o está fuera de horario", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<ReservaResponseDTO> crear(ReservaRequestDTO request, @Parameter(hidden = true) Authentication auth);

    @Operation(summary = "Modificar una reserva", description = "Solo reservas CONFIRMADAS y futuras")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "No existe (o es de otro cliente)", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Choca con otra reserva", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Ya no se puede modificar, capacidad u horario", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<ReservaResponseDTO> actualizar(@Parameter(description = "ID de la reserva") Long id,
                                                  ReservaRequestDTO request, @Parameter(hidden = true) Authentication auth);

    @Operation(summary = "Cancelar una reserva")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva CANCELADA"),
            @ApiResponse(responseCode = "404", description = "No existe (o es de otro cliente)", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Ya estaba cancelada, cumplida o pasó su hora", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<ReservaResponseDTO> cancelar(@Parameter(description = "ID de la reserva") Long id,
                                                @Parameter(hidden = true) Authentication auth);

    @Operation(summary = "Registrar la llegada del cliente",
            description = "Marca la reserva como CUMPLIDA y abre la cuenta de la mesa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva CUMPLIDA, con el ID de la cuenta abierta"),
            @ApiResponse(responseCode = "404", description = "La reserva no existe", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "La mesa todavía tiene una cuenta abierta", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "No está CONFIRMADA o no es para hoy", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<ReservaResponseDTO> registrarLlegada(@Parameter(description = "ID de la reserva") Long id);
}
