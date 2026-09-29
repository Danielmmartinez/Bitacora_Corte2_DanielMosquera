package edu.dosw.restaurante.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tabla "items_pedido". Guarda una copia del nombre y del precio del plato
 * (precio congelado): si el plato cambia o se borra, el pedido no se altera.
 */
@Entity
@Table(name = "items_pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_plato", nullable = false)
    private Long idPlato;

    @Column(name = "nombre_plato", nullable = false, length = 100)
    private String nombrePlato;

    @Column(name = "precio_congelado", nullable = false)
    private Double precioCongelado;

    @Column(nullable = false)
    private Integer cantidad;
}
