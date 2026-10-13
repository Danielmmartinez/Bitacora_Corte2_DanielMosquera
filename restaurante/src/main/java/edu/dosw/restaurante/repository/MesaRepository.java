package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.persistence.entity.MesaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesaRepository extends JpaRepository<MesaEntity, Long> {

    List<MesaEntity> findByEstado(EstadoMesa estado);

    boolean existsByNumero(Integer numero);

    boolean existsByNumeroAndIdNot(Integer numero, Long id);
}
