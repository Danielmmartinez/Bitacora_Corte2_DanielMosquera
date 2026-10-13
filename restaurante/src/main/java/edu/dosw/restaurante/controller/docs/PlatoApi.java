package edu.dosw.restaurante.controller.docs;

import edu.dosw.restaurante.model.dto.request.PlatoRequestDTO;
import edu.dosw.restaurante.model.dto.response.PlatoResponseDTO;
import edu.dosw.restaurante.model.dto.response.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Platos", description = "Administración de la carta del restaurante (gerente)")
public interface PlatoApi {

    @Operation(summary = "Listar todos los platos", description = "Incluye platos disponibles y agotados")
    @ApiResponse(responseCode = "200", description = "Lista de platos")
    ResponseEntity<List<PlatoResponseDTO>> obtenerTodos();

    @Operation(summary = "Obtener un plato por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plato encontrado"),
            @ApiResponse(responseCode = "404", description = "El plato no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<PlatoResponseDTO> obtenerPorId(@Parameter(description = "ID del plato") Long id);

    @Operation(summary = "Crear un nuevo plato")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plato creado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un plato con ese nombre",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<PlatoResponseDTO> crear(PlatoRequestDTO request);

    @Operation(summary = "Actualizar un plato completo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plato actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "El plato no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Otro plato ya tiene ese nombre",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<PlatoResponseDTO> actualizar(@Parameter(description = "ID del plato") Long id, PlatoRequestDTO request);

    @Operation(summary = "Marcar un plato como disponible o agotado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad actualizada"),
            @ApiResponse(responseCode = "400", description = "Falta el parámetro 'disponible' o no es booleano",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "El plato no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<PlatoResponseDTO> cambiarDisponibilidad(@Parameter(description = "ID del plato") Long id,
                                                           @Parameter(description = "true = disponible, false = agotado") boolean disponible);

    @Operation(summary = "Eliminar un plato")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Plato eliminado"),
            @ApiResponse(responseCode = "404", description = "El plato no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<Void> eliminar(@Parameter(description = "ID del plato") Long id);
}
