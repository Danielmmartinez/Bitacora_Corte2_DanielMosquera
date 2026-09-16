package edu.dosw.restaurante.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plato {
    private Long id;
    private String nombre;
    private Double precio;
    private String categoria;
    private Boolean disponible;

    public boolean esValido() {
        return nombre != null && !nombre.isBlank() && precio != null && precio > 0;
    }
}