package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.model.domain.EstadoCuenta;
import edu.dosw.restaurante.persistence.entity.CuentaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CuentaRepository extends JpaRepository<CuentaEntity, Long> {

    List<CuentaEntity> findByEstado(EstadoCuenta estado);

    // Para los reportes: cuentas pagadas en [desde, hasta). Solo las cerradas tienen fechaCierre.
    @Query("SELECT c FROM CuentaEntity c WHERE c.fechaCierre >= :desde AND c.fechaCierre < :hasta ORDER BY c.fechaCierre")
    List<CuentaEntity> findCerradasEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
