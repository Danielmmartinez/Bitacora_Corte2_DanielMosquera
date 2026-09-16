package edu.dosw.restaurante.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mesa {
    private Long id;
    private Integer numero;
    private Integer capacidad;
    private EstadoMesa estado;
    private Boolean cuentaAbierta;

    public Boolean estaDisponible() {
        return EstadoMesa.DISPONIBLE.equals(this.estado);
    }

    public void abrirCuenta() {
        this.estado = EstadoMesa.OCUPADA;
        this.cuentaAbierta = true;
    }

    public void cerrarCuenta() {
        this.estado = EstadoMesa.DISPONIBLE;
        this.cuentaAbierta = false;
    }
}