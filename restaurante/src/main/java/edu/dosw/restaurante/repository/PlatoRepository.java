package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.persistence.entity.PlatoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data genera la implementación en tiempo de ejecución.
 * JpaRepository ya trae save, findById, findAll, existsById, deleteById, count...
 * Los métodos declarados aquí se traducen a SQL a partir de su nombre.
 */
@Repository
public interface PlatoRepository extends JpaRepository<PlatoEntity, Long> {

    // SELECT * FROM platos WHERE disponible = true
    List<PlatoEntity> findByDisponibleTrue();

    // SELECT * FROM platos WHERE disponible = true AND UPPER(categoria) = UPPER(?)
    List<PlatoEntity> findByDisponibleTrueAndCategoriaIgnoreCase(String categoria);

    boolean existsByNombreIgnoreCase(String nombre);

    // Para actualizar: ¿otro plato (distinto id) ya usa ese nombre?
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
