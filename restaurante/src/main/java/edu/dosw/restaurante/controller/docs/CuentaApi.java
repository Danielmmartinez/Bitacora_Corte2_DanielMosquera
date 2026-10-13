package edu.dosw.restaurante.controller.docs;

import edu.dosw.restaurante.model.domain.EstadoCuenta;
import edu.dosw.restaurante.model.dto.request.AbrirCuentaRequestDTO;
import edu.dosw.restaurante.model.dto.request.PagoRequestDTO;
import edu.dosw.restaurante.model.dto.response.CuentaResponseDTO;
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

@Tag(name = "Cuentas", description = "Cuenta de cada mesa: apertura, solicitud y pago")
public interface CuentaApi {

    @Operation(summary = "Listar cuentas", description = "Opcionalmente filtradas por estado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de cuentas con sus pedidos y total"),
            @ApiResponse(responseCode = "400", description = "Estado inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<List<CuentaResponseDTO>> obtenerTodas(@Parameter(description = "Filtro opcional por estado") EstadoCuenta estado);

    @Operation(summary = "Obtener una cuenta por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta encontrada"),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<CuentaResponseDTO> obtenerPorId(@Parameter(description = "ID de la cuenta") Long id);

    @Operation(summary = "Obtener la cuenta abierta de una mesa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta abierta de la mesa"),
            @ApiResponse(responseCode = "404", description = "La mesa no existe o no tiene cuenta abierta",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<CuentaResponseDTO> obtenerAbiertaPorMesa(@Parameter(description = "ID de la mesa") Long idMesa);

    @Operation(summary = "Abrir una cuenta", description = "Ocupa la mesa. Desde ahí se pueden registrar pedidos.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta abierta"),
            @ApiResponse(responseCode = "400", description = "Falta el ID de la mesa",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "La mesa no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "La mesa ya tiene una cuenta abierta",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<CuentaResponseDTO> abrir(AbrirCuentaRequestDTO request);

    @Operation(summary = "Solicitar la cuenta", description = "El cliente pide la cuenta: ABIERTA → EN_PAGO")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta en estado EN_PAGO"),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "La cuenta no está ABIERTA",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<CuentaResponseDTO> solicitarPago(@Parameter(description = "ID de la cuenta") Long id);

    @Operation(summary = "Pagar la cuenta", description = "Congela el total, cierra la cuenta y libera la mesa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta pagada (CERRADA), con el cambio calculado"),
            @ApiResponse(responseCode = "400", description = "Datos de pago inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Ya pagada, pedidos sin entregar o monto insuficiente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<CuentaResponseDTO> pagar(@Parameter(description = "ID de la cuenta") Long id, PagoRequestDTO request);
}
