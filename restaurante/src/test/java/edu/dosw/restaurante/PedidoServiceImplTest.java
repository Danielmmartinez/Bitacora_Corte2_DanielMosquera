package edu.dosw.restaurante;

import edu.dosw.restaurante.model.domain.ItemPedido;
import edu.dosw.restaurante.model.domain.Mesa;
import edu.dosw.restaurante.model.domain.Pedido;
import edu.dosw.restaurante.model.domain.Plato;
import edu.dosw.restaurante.exception.EstadoInvalidoException;
import edu.dosw.restaurante.service.IMesaService;
import edu.dosw.restaurante.service.IPlatoService;
import edu.dosw.restaurante.service.impl.PedidoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock
    private IPlatoService platoService;

    @Mock
    private IMesaService mesaService;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    @Test
    @DisplayName("4. Estado inválido: Intentar pedir un plato no disponible lanza EstadoInvalidoException")
    void crear_PlatoNoDisponible_LanzaEstadoInvalidoException() {
        Long mesaId = 1L;
        Long platoId = 10L;

        Mesa mesa = Mesa.builder().id(mesaId).numero(1).build();
        Plato platoNoDisponible = Plato.builder()
                .id(platoId)
                .nombre("Jugo Natural")
                .precio(5000.0)
                .disponible(false)
                .build();

        when(mesaService.obtenerPorId(mesaId)).thenReturn(mesa);
        when(platoService.obtenerPorId(platoId)).thenReturn(platoNoDisponible);

        ItemPedido item = ItemPedido.builder().idPlato(platoId).cantidad(2).build();
        Pedido pedido = Pedido.builder().idMesa(mesaId).items(List.of(item)).build();

        assertThrows(EstadoInvalidoException.class, () -> {
            pedidoService.crear(pedido);
        });
    }
}