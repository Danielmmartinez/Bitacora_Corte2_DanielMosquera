package edu.dosw.restaurante.repository;

import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.persistence.entity.ItemPedidoEntity;
import edu.dosw.restaurante.persistence.entity.PedidoEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PedidoRepositoryTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ItemPedidoEntity item(long idPlato, int cantidad) {
        return ItemPedidoEntity.builder().idPlato(idPlato).nombrePlato("Plato " + idPlato)
                .precioCongelado(10000.0).cantidad(cantidad).build();
    }

    private PedidoEntity pedido(long idMesa, EstadoPedido estado, ItemPedidoEntity... items) {
        return PedidoEntity.builder().idMesa(idMesa).estado(estado).timestamp(LocalDateTime.now())
                .items(new ArrayList<>(List.of(items))).build();
    }

    private long contarItemsEnBD() {
        return ((Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM items_pedido").getSingleResult()).longValue();
    }

    // Vacía el contexto de persistencia para que la siguiente lectura vaya realmente a la BD
    private void sincronizarYLimpiar() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void save_GuardaLosItemsEnCascada() {
        PedidoEntity guardado = pedidoRepository.save(pedido(1, EstadoPedido.RECIBIDO, item(1, 2), item(2, 1)));
        sincronizarYLimpiar();

        PedidoEntity leido = pedidoRepository.findById(guardado.getId()).orElseThrow();

        assertEquals(2, leido.getItems().size());
        assertTrue(leido.getItems().stream().allMatch(i -> i.getId() != null));
        assertEquals(2, contarItemsEnBD());
    }

    @Test
    void quitarItemDeLaLista_LoBorraDeLaBD_OrphanRemoval() {
        PedidoEntity guardado = pedidoRepository.save(pedido(1, EstadoPedido.RECIBIDO, item(1, 2), item(2, 1)));
        sincronizarYLimpiar();

        PedidoEntity leido = pedidoRepository.findById(guardado.getId()).orElseThrow();
        leido.getItems().remove(0);
        sincronizarYLimpiar();

        assertEquals(1, contarItemsEnBD());
    }

    @Test
    void deleteById_BorraTambienLosItems() {
        PedidoEntity guardado = pedidoRepository.save(pedido(1, EstadoPedido.RECIBIDO, item(1, 1)));
        sincronizarYLimpiar();

        pedidoRepository.deleteById(guardado.getId());
        sincronizarYLimpiar();

        assertEquals(0, contarItemsEnBD());
        assertTrue(pedidoRepository.findById(guardado.getId()).isEmpty());
    }

    @Test
    void findByIdMesa_Y_FindByEstado_TraenItems() {
        pedidoRepository.save(pedido(1, EstadoPedido.RECIBIDO, item(1, 1)));
        pedidoRepository.save(pedido(1, EstadoPedido.EN_PREPARACION, item(2, 3)));
        pedidoRepository.save(pedido(2, EstadoPedido.RECIBIDO, item(3, 1)));
        sincronizarYLimpiar();

        List<PedidoEntity> mesa1 = pedidoRepository.findByIdMesa(1L);
        List<PedidoEntity> recibidos = pedidoRepository.findByEstado(EstadoPedido.RECIBIDO);

        assertEquals(2, mesa1.size());
        assertEquals(2, recibidos.size());
        // Gracias a @EntityGraph los ítems ya vienen cargados
        assertEquals(1, recibidos.get(0).getItems().size());
    }
}
