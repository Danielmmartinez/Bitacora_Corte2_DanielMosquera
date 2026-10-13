package edu.dosw.restaurante.controller.docs;

import edu.dosw.restaurante.model.dto.request.EntradaVehiculoRequestDTO;
import edu.dosw.restaurante.model.dto.response.EstadoParqueaderoResponseDTO;
import edu.dosw.restaurante.model.dto.response.RegistroVehiculoResponseDTO;
import edu.dosw.restaurante.model.dto.response.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Parqueadero", description = "Entrada y salida de vehículos, cupos y cobro por hora o fracción")
public interface ParqueaderoApi {

    @Operation(summary = "Registrar la entrada de un vehículo",
            description = "El tipo (CARRO/MOTO) se deduce de la placa. Acepta 'abc-123', 'ABC 12D'...")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entrada registrada"),
            @ApiResponse(responseCode = "400", description = "Placa con formato inválido", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Ese vehículo ya está adentro", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Parqueadero lleno", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<RegistroVehiculoResponseDTO> registrarEntrada(EntradaVehiculoRequestDTO request);

    @Operation(summary = "Registrar la salida y calcular el cobro",
            description = "Se cobra por hora o fracción (mínimo 1 hora) según la tarifa del tipo de vehículo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Salida registrada con su cobro"),
            @ApiResponse(responseCode = "404", description = "No hay un vehículo con esa placa adentro", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<RegistroVehiculoResponseDTO> registrarSalida(@Parameter(description = "Placa, ej. ABC123") String placa);

    @Operation(summary = "Cupos disponibles y vehículos adentro")
    @ApiResponse(responseCode = "200", description = "Estado actual")
    ResponseEntity<EstadoParqueaderoResponseDTO> obtenerEstado();

    @Operation(summary = "Registros de un día", description = "Vehículos que entraron ese día (por defecto, hoy)")
    @ApiResponse(responseCode = "200", description = "Registros ordenados por hora de entrada")
    ResponseEntity<List<RegistroVehiculoResponseDTO>> obtenerRegistros(@Parameter(description = "Fecha (AAAA-MM-DD)") LocalDate fecha);

    @Operation(summary = "Obtener un registro por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<RegistroVehiculoResponseDTO> obtenerPorId(@Parameter(description = "ID del registro") Long id);
}
