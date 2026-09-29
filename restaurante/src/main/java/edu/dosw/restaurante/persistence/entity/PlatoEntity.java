package edu.dosw.restaurante.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tabla "platos". Clase de persistencia: solo describe cómo se guarda un Plato,
 * no tiene lógica de negocio (esa vive en model/domain/Plato).
 */
@Entity
@Table(name = "platos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // la BD asigna el ID al hacer INSERT
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Double precio;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false)
    private Boolean disponible;
}
