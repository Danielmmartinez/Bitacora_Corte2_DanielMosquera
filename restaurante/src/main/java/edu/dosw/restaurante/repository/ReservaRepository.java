package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.model.domain.EstadoReserva;
import edu.dosw.restaurante.persistence.entity.ReservaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<ReservaEntity, Long> {

    /**
     * Candidatas a cruzarse: reservas de la mesa, en ese estado, que empiezan dentro de
     * (inicio - duración, inicio + duración). Consulta derivada del nombre del método.
     */
    List<ReservaEntity> findByIdMesaAndEstadoAndFechaHoraAfterAndFechaHoraBefore(
            Long idMesa, EstadoReserva estado, LocalDateTime desde, LocalDateTime hasta);

    List<ReservaEntity> findByEmailClienteIgnoreCaseOrderByFechaHoraDesc(String emailCliente);

    /**
     * Consulta escrita a mano en JPQL: se parece a SQL, pero trabaja con ENTIDADES y sus
     * atributos (ReservaEntity, fechaHora), no con tablas y columnas (reservas, fecha_hora).
     */
    @Query("SELECT r FROM ReservaEntity r WHERE r.fechaHora >= :desde AND r.fechaHora < :hasta ORDER BY r.fechaHora")
    List<ReservaEntity> findEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
