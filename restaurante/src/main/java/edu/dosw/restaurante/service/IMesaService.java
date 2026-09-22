package edu.dosw.restaurante.service;

import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.model.domain.Mesa;

import java.util.List;

public interface IMesaService {
    List<Mesa> obtenerTodas();
    Mesa obtenerPorId(Long id);
    Mesa crear(Mesa mesa);
    Mesa cambiarEstado(Long id, EstadoMesa nuevoEstado);
    void eliminar(Long id);
}