package edu.dosw.restaurante.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {
    private Long id;
    private Long idPlato;
    private String nombrePlato;
    private Double precioCongelado;
    private Integer cantidad;

    public Double subtotal() {
        if (precioCongelado == null || cantidad == null) {
            return 0.0;
        }
        return precioCongelado * cantidad;
    }
}