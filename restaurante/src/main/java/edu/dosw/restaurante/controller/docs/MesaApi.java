package edu.dosw.restaurante.controller.docs;

import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.model.dto.request.MesaRequestDTO;
import edu.dosw.restaurante.model.dto.response.MesaResponseDTO;
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

@Tag(name = "Mesas", description = "Gestión del salón y sus mesas")
public interface MesaApi {

    @Operation(summary = "Listar mesas", description = "Opcionalmente filtradas por estado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de mesas"),
            @ApiResponse(responseCode = "400", description = "Estado inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<List<MesaResponseDTO>> obtenerTodas(@Parameter(description = "Filtro opcional por estado") EstadoMesa estado);

    @Operation(summary = "Obtener una mesa por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mesa encontrada"),
            @ApiResponse(responseCode = "404", description = "La mesa no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<MesaResponseDTO> obtenerPorId(@Parameter(description = "ID de la mesa") Long id);

    @Operation(summary = "Registrar una nueva mesa")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mesa creada"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe una mesa con ese número",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<MesaResponseDTO> crear(MesaRequestDTO request);

    @Operation(summary = "Actualizar número y capacidad de una mesa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mesa actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "La mesa no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Otra mesa ya tiene ese número",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<MesaResponseDTO> actualizar(@Parameter(description = "ID de la mesa") Long id, MesaRequestDTO request);

    @Operation(summary = "Cambiar el estado de una mesa (DISPONIBLE ↔ RESERVADA)",
            description = "Para ocupar la mesa se abre una cuenta (POST /cuentas); se libera al pagarla")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Estado ausente o inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "La mesa no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Se pidió OCUPADA, o la mesa tiene una cuenta abierta",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<MesaResponseDTO> cambiarEstado(@Parameter(description = "ID de la mesa") Long id,
                                                  @Parameter(description = "Nuevo estado") EstadoMesa estado);

    @Operation(summary = "Eliminar una mesa")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mesa eliminada"),
            @ApiResponse(responseCode = "404", description = "La mesa no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "La mesa tiene una cuenta abierta",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<Void> eliminar(@Parameter(description = "ID de la mesa") Long id);
}
