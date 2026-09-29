package edu.dosw.restaurante.service;

import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.domain.Pedido;

import java.time.LocalDateTime;
import java.util.List;

public interface IPedidoService {
    List<Pedido> obtenerTodos();
    List<Pedido> obtenerPorEstado(EstadoPedido estado);
    Pedido obtenerPorId(Long id);
    List<Pedido> obtenerPorMesa(Long idMesa);
    List<Pedido> obtenerPorCuenta(Long idCuenta);
    List<Pedido> obtenerCreadosEntre(LocalDateTime desde, LocalDateTime hasta);
    Pedido crear(Pedido pedido);
    Pedido actualizar(Long id, Pedido pedidoActualizado);
    Pedido cambiarEstado(Long id, EstadoPedido nuevoEstado);
    void eliminar(Long id);
}
