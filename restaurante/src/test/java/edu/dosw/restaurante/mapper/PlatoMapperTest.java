package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.Plato;
import edu.dosw.restaurante.model.dto.request.PlatoRequestDTO;
import edu.dosw.restaurante.model.dto.response.PlatoResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlatoMapperTest {

    private PlatoMapper platoMapper;

    @BeforeEach
    void setUp() {
        platoMapper = new PlatoMapperImpl();
    }

    @Test
    void toDomain_Exito() {
        PlatoRequestDTO dto = PlatoRequestDTO.builder()
                .nombre("Pizza")
                .precio(30000.0)
                .categoria("Italiana")
                .disponible(true)
                .build();

        Plato domain = platoMapper.toDomain(dto);

        assertNotNull(domain);
        assertEquals("Pizza", domain.getNombre());
        assertEquals(30000.0, domain.getPrecio());
    }

    @Test
    void toDomain_Null() {
        assertNull(platoMapper.toDomain(null));
    }

    @Test
    void toResponse_Exito() {
        Plato domain = Plato.builder()
                .id(1L)
                .nombre("Pizza")
                .precio(30000.0)
                .categoria("Italiana")
                .disponible(true)
                .build();

        PlatoResponseDTO dto = platoMapper.toResponse(domain);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Pizza", dto.getNombre());
    }

    @Test
    void toResponse_Null() {
        assertNull(platoMapper.toResponse(null));
    }

    @Test
    void toResponseList_ExitoYNull() {
        Plato domain = Plato.builder().id(1L).nombre("Sopa").build();
        List<PlatoResponseDTO> dtoList = platoMapper.toResponseList(List.of(domain));

        assertNotNull(dtoList);
        assertEquals(1, dtoList.size());

        assertNull(platoMapper.toResponseList(null));
    }
}