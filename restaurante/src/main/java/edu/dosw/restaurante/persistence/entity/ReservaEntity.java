package edu.dosw.restaurante.persistence.entity;

import edu.dosw.restaurante.model.domain.EstadoReserva;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Tabla "reservas". El índice (id_mesa, fecha_hora) acelera la búsqueda de reservas que se cruzan.
 */
@Entity
@Table(name = "reservas", indexes = @Index(name = "idx_reserva_mesa_fecha", columnList = "id_mesa, fecha_hora"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_mesa", nullable = false)
    private Long idMesa;

    @Column(name = "nombre_cliente", nullable = false, length = 100)
    private String nombreCliente;

    @Column(name = "email_cliente", length = 120)
    private String emailCliente;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private Integer comensales;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "id_cuenta")
    private Long idCuenta;
}
