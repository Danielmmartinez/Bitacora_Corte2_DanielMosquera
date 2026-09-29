package edu.dosw.restaurante.controller.docs;

import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.dto.request.PedidoRequestDTO;
import edu.dosw.restaurante.model.dto.response.EventoPedidoResponseDTO;
import edu.dosw.restaurante.model.dto.response.PedidoResponseDTO;
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

@Tag(name = "Pedidos", description = "Flujo de pedidos y cocina")
public interface PedidoApi {

    @Operation(summary = "Listar pedidos", description = "Opcionalmente filtrados por estado (ej. panel de cocina)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pedidos"),
            @ApiResponse(responseCode = "400", description = "Estado inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<List<PedidoResponseDTO>> obtenerTodos(@Parameter(description = "Filtro opcional por estado") EstadoPedido estado);

    @Operation(summary = "Obtener un pedido por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "El pedido no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<PedidoResponseDTO> obtenerPorId(@Parameter(description = "ID del pedido") Long id);

    @Operation(summary = "Listar los pedidos de una mesa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedidos de la mesa"),
            @ApiResponse(responseCode = "404", description = "La mesa no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<List<PedidoResponseDTO>> obtenerPorMesa(@Parameter(description = "ID de la mesa") Long idMesa);

    @Operation(summary = "Historial de un pedido",
            description = "Eventos guardados en MongoDB: creación, modificaciones, cambios de estado y eliminación. "
                    + "Se conserva aunque el pedido haya sido eliminado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eventos en orden cronológico (vacío si no hay)"),
            @ApiResponse(responseCode = "503", description = "MongoDB no está disponible",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<List<EventoPedidoResponseDTO>> obtenerHistorial(@Parameter(description = "ID del pedido") Long id);

    @Operation(summary = "Registrar un pedido", description = "La mesa debe tener la cuenta abierta; el precio de cada plato se congela")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado en estado RECIBIDO"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "La mesa o algún plato no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Mesa sin cuenta abierta o plato agotado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<PedidoResponseDTO> crear(PedidoRequestDTO request);

    @Operation(summary = "Reemplazar mesa e ítems de un pedido", description = "Solo mientras el pedido está RECIBIDO")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "El pedido, la mesa o algún plato no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "El pedido ya no puede modificarse",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<PedidoResponseDTO> actualizar(@Parameter(description = "ID del pedido") Long id, PedidoRequestDTO request);

    @Operation(summary = "Cambiar el estado de un pedido",
            description = "RECIBIDO → EN_PREPARACION | CANCELADO, EN_PREPARACION → LISTO, LISTO → ENTREGADO")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Estado ausente o inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "El pedido no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "Transición de estado no permitida",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<PedidoResponseDTO> cambiarEstado(@Parameter(description = "ID del pedido") Long id,
                                                    @Parameter(description = "Nuevo estado") EstadoPedido estado);

    @Operation(summary = "Eliminar un pedido", description = "Solo mientras el pedido está RECIBIDO")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pedido eliminado"),
            @ApiResponse(responseCode = "404", description = "El pedido no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "422", description = "El pedido ya pasó a cocina",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    ResponseEntity<Void> eliminar(@Parameter(description = "ID del pedido") Long id);
}
