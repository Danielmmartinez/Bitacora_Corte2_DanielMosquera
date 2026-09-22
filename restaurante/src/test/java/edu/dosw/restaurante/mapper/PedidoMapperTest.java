package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.domain.ItemPedido;
import edu.dosw.restaurante.model.domain.Pedido;
import edu.dosw.restaurante.model.dto.request.ItemPedidoRequestDTO;
import edu.dosw.restaurante.model.dto.request.PedidoRequestDTO;
import edu.dosw.restaurante.model.dto.response.PedidoResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PedidoMapperTest {

    private PedidoMapper pedidoMapper;

    @BeforeEach
    void setUp() {
        pedidoMapper = new PedidoMapperImpl();
    }

    @Test
    void toDomain_Exito() {
        ItemPedidoRequestDTO itemDTO = ItemPedidoRequestDTO.builder().idPlato(1L).cantidad(2).build();
        PedidoRequestDTO dto = PedidoRequestDTO.builder().idMesa(3L).items(List.of(itemDTO)).build();

        Pedido domain = pedidoMapper.toDomain(dto);

        assertNotNull(domain);
        assertEquals(3L, domain.getIdMesa());
        assertEquals(EstadoPedido.RECIBIDO, domain.getEstado());
        assertNotNull(domain.getTimestamp());
        assertEquals(1, domain.getItems().size());
    }

    @Test
    void toDomain_Null() {
        assertNull(pedidoMapper.toDomain(null));
        assertNull(pedidoMapper.itemToDomain(null));
    }

    @Test
    void toResponse_Exito() {
        ItemPedido item = ItemPedido.builder()
                .id(10L)
                .idPlato(1L)
                .nombrePlato("Jugos")
                .precioCongelado(5000.0)
                .cantidad(2)
                .build();

        Pedido domain = Pedido.builder()
                .id(100L)
                .idMesa(2L)
                .estado(EstadoPedido.EN_PREPARACION)
                .timestamp(LocalDateTime.now())
                .items(List.of(item))
                .build();

        PedidoResponseDTO response = pedidoMapper.toResponse(domain);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("EN_PREPARACION", response.getEstado());
        assertEquals(1, response.getItems().size());
        assertEquals(10000.0, response.getItems().get(0).getSubtotal());
    }

    @Test
    void toResponse_Null() {
        assertNull(pedidoMapper.toResponse(null));
        assertNull(pedidoMapper.itemToResponse(null));
    }

    @Test
    void toResponseList_ExitoYNull() {
        Pedido domain = Pedido.builder().id(1L).build();
        List<PedidoResponseDTO> list = pedidoMapper.toResponseList(List.of(domain));

        assertNotNull(list);
        assertEquals(1, list.size());

        assertNull(pedidoMapper.toResponseList(null));
    }
}