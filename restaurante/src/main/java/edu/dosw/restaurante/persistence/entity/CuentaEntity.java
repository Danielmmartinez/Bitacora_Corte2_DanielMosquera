package edu.dosw.restaurante.persistence.entity;

import edu.dosw.restaurante.model.domain.EstadoCuenta;
import edu.dosw.restaurante.model.domain.MetodoPago;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Tabla "cuentas". Los pedidos no se guardan aquí: cada pedido tiene la columna id_cuenta.
 */
@Entity
@Table(name = "cuentas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_mesa", nullable = false)
    private Long idMesa;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    // Los siguientes quedan en NULL hasta que la cuenta se paga
    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    private Double total;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "metodo_pago", length = 20)
    private MetodoPago metodoPago;

    @Column(name = "monto_recibido")
    private Double montoRecibido;
}
