package edu.dosw.restaurante.service;

import edu.dosw.restaurante.domain.Plato;

import java.util.List;

public interface IPlatoService {
    List<Plato> obtenerTodos();
    Plato obtenerPorId(Long id);
    Plato crear(Plato plato);
    Plato actualizar(Long id, Plato platoActualizado);
    void eliminar(Long id);
}