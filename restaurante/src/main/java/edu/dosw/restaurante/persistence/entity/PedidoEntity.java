package edu.dosw.restaurante.persistence.entity;

import edu.dosw.restaurante.model.domain.EstadoPedido;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla "pedidos". Sus ítems viven en la tabla "items_pedido" (relación 1 a N).
 */
@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referencia por ID (no @ManyToOne): el historial de pedidos no depende del ciclo de vida de la mesa
    @Column(name = "id_mesa", nullable = false)
    private Long idMesa;

    // Cuenta a la que se cobra el pedido. Nullable solo para no romper BDs creadas antes de existir las cuentas.
    @Column(name = "id_cuenta")
    private Long idCuenta;

    // cascade = ALL: guardar/borrar el pedido guarda/borra sus ítems.
    // orphanRemoval: un ítem que se quita de la lista se borra de la BD.
    // @JoinColumn: la columna pedido_id vive en items_pedido (FK hacia pedidos).
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pedido_id", nullable = false)
    @Builder.Default
    private List<ItemPedidoEntity> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime timestamp;
}
