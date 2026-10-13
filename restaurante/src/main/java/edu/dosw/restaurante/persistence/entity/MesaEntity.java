package edu.dosw.restaurante.persistence.entity;

import edu.dosw.restaurante.model.domain.EstadoMesa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Tabla "mesas".
 */
@Entity
@Table(name = "mesas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MesaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer numero;

    @Column(nullable = false)
    private Integer capacidad;

    // STRING guarda "OCUPADA"; ORDINAL guardaría 1 y se rompería si se reordena el enum.
    // VARCHAR evita que H2 cree un tipo ENUM nativo que ddl-auto=update no amplía al agregar valores.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoMesa estado;

    @Column(name = "cuenta_abierta", nullable = false)
    private Boolean cuentaAbierta;

    // ID de la cuenta abierta en este momento (NULL si la mesa no tiene cuenta)
    @Column(name = "id_cuenta_abierta")
    private Long idCuentaAbierta;
}
