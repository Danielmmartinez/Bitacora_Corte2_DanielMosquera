package edu.dosw.restaurante.service;

import edu.dosw.restaurante.domain.EstadoPedido;
import edu.dosw.restaurante.domain.Pedido;

import java.util.List;

public interface IPedidoService {
    List<Pedido> obtenerTodos();
    Pedido obtenerPorId(Long id);
    List<Pedido> obtenerPorMesa(Long idMesa);
    Pedido crear(Pedido pedido);
    Pedido cambiarEstado(Long id, EstadoPedido nuevoEstado);
    void eliminar(Long id);
}