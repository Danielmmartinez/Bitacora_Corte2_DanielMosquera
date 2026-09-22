package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.model.dto.request.MesaRequestDTO;
import edu.dosw.restaurante.model.dto.response.MesaResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MesaMapperTest {

    private MesaMapper mesaMapper;

    @BeforeEach
    void setUp() {
        mesaMapper = new MesaMapperImpl();
    }

    @Test
    void toDomain_Exito() {
        MesaRequestDTO dto = MesaRequestDTO.builder()
                .numero(5)
                .capacidad(4)
                .build();

        Mesa domain = mesaMapper.toDomain(dto);

        assertNotNull(domain);
        assertEquals(5, domain.getNumero());
        assertEquals(EstadoMesa.DISPONIBLE, domain.getEstado());
        assertFalse(domain.getCuentaAbierta());
    }

    @Test
    void toDomain_Null() {
        assertNull(mesaMapper.toDomain(null));
    }

    @Test
    void toResponse_Exito() {
        Mesa domain = Mesa.builder()
                .id(1L)
                .numero(2)
                .capacidad(2)
                .estado(EstadoMesa.OCUPADA)
                .cuentaAbierta(true)
                .build();

        MesaResponseDTO dto = mesaMapper.toResponse(domain);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("OCUPADA", dto.getEstado());
    }

    @Test
    void toResponse_Null() {
        assertNull(mesaMapper.toResponse(null));
    }

    @Test
    void toResponseList_ExitoYNull() {
        Mesa domain = Mesa.builder().id(1L).numero(1).build();
        List<MesaResponseDTO> list = mesaMapper.toResponseList(List.of(domain));

        assertNotNull(list);
        assertEquals(1, list.size());

        assertNull(mesaMapper.toResponseList(null));
    }
}