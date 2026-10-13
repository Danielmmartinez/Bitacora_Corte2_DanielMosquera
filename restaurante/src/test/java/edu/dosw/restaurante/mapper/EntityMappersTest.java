package edu.dosw.restaurante.mapper;

import edu.dosw.restaurante.model.domain.*;
import edu.dosw.restaurante.persistence.entity.ItemPedidoEntity;
import edu.dosw.restaurante.persistence.entity.MesaEntity;
import edu.dosw.restaurante.persistence.entity.PedidoEntity;
import edu.dosw.restaurante.persistence.entity.PlatoEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityMappersTest {

    private final PlatoEntityMapper platoMapper = new PlatoEntityMapperImpl();
    private final MesaEntityMapper mesaMapper = new MesaEntityMapperImpl();
    private final PedidoEntityMapper pedidoMapper = new PedidoEntityMapperImpl();

    @Test
    void plato_IdaYVuelta_ConservaTodosLosCampos() {
        Plato original = Plato.builder().id(1L).nombre("Nigiri").precio(12000.0).categoria("Sushi").disponible(true).build();

        PlatoEntity entidad = platoMapper.toEntity(original);
        Plato vuelta = platoMapper.toDomain(entidad);

        assertEquals(original, vuelta);
        assertNull(platoMapper.toEntity(null));
        assertNull(platoMapper.toDomain(null));
    }

    @Test
    void mesa_IdaYVuelta_ConservaTodosLosCampos() {
        Mesa original = Mesa.builder().id(2L).numero(5).capacidad(4).estado(EstadoMesa.OCUPADA).cuentaAbierta(true).build();

        MesaEntity entidad = mesaMapper.toEntity(original);

        assertEquals(EstadoMesa.OCUPADA, entidad.getEstado());
        assertEquals(original, mesaMapper.toDomain(entidad));
        assertNull(mesaMapper.toEntity(null));
        assertNull(mesaMapper.toDomain(null));
    }

    @Test
    void pedido_ConvierteTambienLaListaDeItems() {
        ItemPedidoEntity item = ItemPedidoEntity.builder().id(10L).idPlato(1L).nombrePlato("Nigiri")
                .precioCongelado(12000.0).cantidad(2).build();
        PedidoEntity entidad = PedidoEntity.builder().id(3L).idMesa(1L).estado(EstadoPedido.LISTO)
                .timestamp(LocalDateTime.now()).items(List.of(item)).build();

        Pedido dominio = pedidoMapper.toDomain(entidad);

        assertEquals(3L, dominio.getId());
        assertEquals(1, dominio.getItems().size());
        assertEquals(24000.0, dominio.calcularTotal());

        PedidoEntity vuelta = pedidoMapper.toEntity(dominio);
        assertEquals(10L, vuelta.getItems().get(0).getId());
        assertEquals("Nigiri", vuelta.getItems().get(0).getNombrePlato());
        assertNull(pedidoMapper.toEntity(null));
        assertNull(pedidoMapper.toDomain(null));
        assertNull(pedidoMapper.itemToEntity(null));
        assertNull(pedidoMapper.itemToDomain(null));
    }
}
