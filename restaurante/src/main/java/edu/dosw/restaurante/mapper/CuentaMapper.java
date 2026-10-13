package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.Cuenta;
import edu.dosw.restaurante.model.dto.response.CuentaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Dominio → DTO. Reutiliza PedidoMapper para convertir la lista de pedidos de la cuenta.
 */
@Mapper(componentModel = "spring", uses = PedidoMapper.class)
public interface CuentaMapper {

    @Mapping(target = "total", expression = "java(cuenta.calcularTotal())")
    @Mapping(target = "cambio", expression = "java(cuenta.calcularCambio())")
    CuentaResponseDTO toResponse(Cuenta cuenta);

    List<CuentaResponseDTO> toResponseList(List<Cuenta> cuentas);
}
