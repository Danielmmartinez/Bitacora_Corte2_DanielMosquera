package edu.dosw.restaurante.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.image.BufferedImage;

@Entity
@Table(name = "platos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 2)
    private Double precio;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false)
    private Boolean disponible
}