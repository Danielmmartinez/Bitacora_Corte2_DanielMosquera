package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.domain.MetodoPago;
import edu.dosw.restaurante.model.domain.reporte.IngresoDiario;
import edu.dosw.restaurante.model.domain.reporte.PlatoVendido;
import edu.dosw.restaurante.model.domain.reporte.ReporteIngresos;
import edu.dosw.restaurante.model.domain.reporte.ResumenDia;
import edu.dosw.restaurante.model.dto.response.IngresoDiarioResponseDTO;
import edu.dosw.restaurante.model.dto.response.PlatoVendidoResponseDTO;
import edu.dosw.restaurante.model.dto.response.ReporteIngresosResponseDTO;
import edu.dosw.restaurante.model.dto.response.ResumenDiaResponseDTO;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Map;

/**
 * MapStruct también trabaja con records: los construye con su constructor.
 * Los mapas con claves enum se convierten a claves String (el nombre del enum).
 */
@Mapper(componentModel = "spring")
public interface ReporteMapper {

    ResumenDiaResponseDTO toResponse(ResumenDia resumen);

    List<PlatoVendidoResponseDTO> toPlatosResponse(List<PlatoVendido> platos);

    PlatoVendidoResponseDTO toResponse(PlatoVendido plato);

    ReporteIngresosResponseDTO toResponse(ReporteIngresos ingresos);

    IngresoDiarioResponseDTO toResponse(IngresoDiario ingreso);

    Map<String, Long> estadosToTexto(Map<EstadoPedido, Long> porEstado);

    Map<String, Double> metodosToTexto(Map<MetodoPago, Double> porMetodo);
}
