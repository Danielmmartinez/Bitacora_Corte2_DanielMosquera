package edu.dosw.restaurante.controller.docs;

import edu.dosw.restaurante.model.dto.response.PlatoResponseDTO;
import edu.dosw.restaurante.model.dto.response.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@SecurityRequirements // público: no requiere token
@Tag(name = "Menú", description = "Consulta de la carta — vista del cliente (solo platos disponibles)")
public interface MenuApi {

    @Operation(summary = "Ver la carta del restaurante")
    @ApiResponse(responseCode = "200", description = "Platos disponibles")
    ResponseEntity<List<PlatoResponseDTO>> verCarta();

    @Operation(summary = "Ver el detalle de un plato disponible")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plato encontrado"),
            @ApiResponse(responseCode = "404", description = "El plato no existe o no está disponible",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<PlatoResponseDTO> verDetalle(@Parameter(description = "ID del plato") Long id);

    @Operation(summary = "Ver la carta filtrada por categoría")
    @ApiResponse(responseCode = "200", description = "Platos disponibles de la categoría")
    ResponseEntity<List<PlatoResponseDTO>> porCategoria(@Parameter(description = "Categoría, ej. ROLLS") String categoria);
}
