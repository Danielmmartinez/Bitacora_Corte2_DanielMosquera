package edu.dosw.restaurante.service;

import edu.dosw.restaurante.model.domain.Plato;

import java.util.List;

public interface IPlatoService {
    List<Plato> obtenerTodos();
    List<Plato> obtenerDisponibles();
    List<Plato> obtenerDisponiblesPorCategoria(String categoria);
    Plato obtenerPorId(Long id);
    Plato obtenerDisponiblePorId(Long id);
    Plato crear(Plato plato);
    Plato actualizar(Long id, Plato platoActualizado);
    Plato cambiarDisponibilidad(Long id, boolean disponible);
    void eliminar(Long id);
}
