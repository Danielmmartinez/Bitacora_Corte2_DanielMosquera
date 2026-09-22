package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.Plato;
import edu.dosw.restaurante.model.dto.request.PlatoRequestDTO;
import edu.dosw.restaurante.model.dto.response.PlatoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class PlatoMapperTest {

    private final PlatoMapper mapper = Mappers.getMapper(PlatoMapper.class);

    @Test
    @DisplayName("Prueba de mapeo: PlatoRequestDTO a Dominio")
    void toDomain_MapeaCorrectamente() {
        PlatoRequestDTO dto = PlatoRequestDTO.builder()
                .nombre("Bandeja Paisa")
                .precio(35000.0)
                .categoria("PLATO_FUERTE")
                .disponible(true)
                .build();

        Plato domain = mapper.toDomain(dto);

        assertNotNull(domain);
        assertEquals("Bandeja Paisa", domain.getNombre());
        assertEquals(35000.0, domain.getPrecio());
    }

    @Test
    @DisplayName("Prueba de mapeo: Dominio a PlatoResponseDTO")
    void toResponse_MapeaCorrectamente() {
        Plato domain = Plato.builder()
                .id(1L)
                .nombre("Bandeja Paisa")
                .precio(35000.0)
                .categoria("PLATO_FUERTE")
                .disponible(true)
                .build();

        PlatoResponseDTO dto = mapper.toResponse(domain);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Bandeja Paisa", dto.getNombre());
    }
}