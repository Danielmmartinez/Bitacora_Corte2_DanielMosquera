package edu.dosw.restaurante.persistence.entity;

import edu.dosw.restaurante.model.domain.TipoVehiculo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Tabla "registros_vehiculo". Un registro con salida = NULL es un vehículo que sigue adentro.
 */
@Entity
@Table(name = "registros_vehiculo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroVehiculoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String placa;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private TipoVehiculo tipo;

    @Column(nullable = false)
    private LocalDateTime entrada;

    private LocalDateTime salida;

    private Double cobro;
}
