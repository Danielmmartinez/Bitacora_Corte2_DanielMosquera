package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.persistence.entity.RegistroVehiculoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroVehiculoRepository extends JpaRepository<RegistroVehiculoEntity, Long> {

    // "SalidaIsNull" → WHERE salida IS NULL: el vehículo sigue adentro
    Optional<RegistroVehiculoEntity> findByPlacaAndSalidaIsNull(String placa);

    boolean existsByPlacaAndSalidaIsNull(String placa);

    long countBySalidaIsNull();

    List<RegistroVehiculoEntity> findBySalidaIsNullOrderByEntradaAsc();

    @Query("SELECT r FROM RegistroVehiculoEntity r WHERE r.entrada >= :desde AND r.entrada < :hasta ORDER BY r.entrada")
    List<RegistroVehiculoEntity> findEntradasEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT r FROM RegistroVehiculoEntity r WHERE r.salida >= :desde AND r.salida < :hasta")
    List<RegistroVehiculoEntity> findSalidasEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
