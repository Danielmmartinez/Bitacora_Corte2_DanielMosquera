package edu.dosw.restaurante.service;

import edu.dosw.restaurante.model.domain.EventoPedido;

import java.util.List;

public interface IEventoPedidoService {
    void registrar(EventoPedido evento);
    List<EventoPedido> obtenerHistorial(Long idPedido);
}
