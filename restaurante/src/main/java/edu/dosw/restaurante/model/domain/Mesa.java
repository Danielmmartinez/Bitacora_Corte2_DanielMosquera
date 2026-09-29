package edu.dosw.restaurante.model.domain;

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
    private Long idCuentaAbierta;

    public Boolean estaDisponible() {
        return EstadoMesa.DISPONIBLE.equals(this.estado);
    }

    public boolean tieneCuentaAbierta() {
        return Boolean.TRUE.equals(cuentaAbierta);
    }

    public void abrirCuenta(Long idCuenta) {
        this.estado = EstadoMesa.OCUPADA;
        this.cuentaAbierta = true;
        this.idCuentaAbierta = idCuenta;
    }

    public void cerrarCuenta() {
        this.estado = EstadoMesa.DISPONIBLE;
        this.cuentaAbierta = false;
        this.idCuentaAbierta = null;
    }
}
