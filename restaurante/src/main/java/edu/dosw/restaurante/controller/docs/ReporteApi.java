package edu.dosw.restaurante.controller.docs;

import edu.dosw.restaurante.model.dto.response.PlatoVendidoResponseDTO;
import edu.dosw.restaurante.model.dto.response.ReporteIngresosResponseDTO;
import edu.dosw.restaurante.model.dto.response.ResumenDiaResponseDTO;
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

@Tag(name = "Reportes", description = "Indicadores del negocio calculados con Streams (solo GERENTE)")
public interface ReporteApi {

    @Operation(summary = "Resumen del día",
            description = "Pedidos, ingresos, ticket promedio, reservas, parqueadero y ocupación actual")
    @ApiResponse(responseCode = "200", description = "Resumen")
    ResponseEntity<ResumenDiaResponseDTO> resumen(@Parameter(description = "Fecha (AAAA-MM-DD), por defecto hoy") LocalDate fecha);

    @Operation(summary = "Platos más vendidos", description = "Ranking por unidades de los pedidos no cancelados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking"),
            @ApiResponse(responseCode = "400", description = "Rango o límite inválido", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<List<PlatoVendidoResponseDTO>> platosMasVendidos(
            @Parameter(description = "Desde (por defecto hoy)") LocalDate desde,
            @Parameter(description = "Hasta, inclusive (por defecto = desde)") LocalDate hasta,
            @Parameter(description = "Cuántos platos (1 a 50)") int limite);

    @Operation(summary = "Ingresos por rango de fechas",
            description = "Total del restaurante (cuentas pagadas) y del parqueadero; desglose por día, método de pago y categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte de ingresos"),
            @ApiResponse(responseCode = "400", description = "Rango inválido (desde > hasta o más de 366 días)", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<ReporteIngresosResponseDTO> ingresos(@Parameter(description = "Desde (por defecto hoy)") LocalDate desde,
                                                        @Parameter(description = "Hasta, inclusive (por defecto = desde)") LocalDate hasta);
}
