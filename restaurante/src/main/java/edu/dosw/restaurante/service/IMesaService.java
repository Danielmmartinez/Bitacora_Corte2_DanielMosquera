package edu.dosw.restaurante.service;

import edu.dosw.restaurante.model.domain.EstadoMesa;
import edu.dosw.restaurante.model.domain.Mesa;

import java.util.List;

public interface IMesaService {
    List<Mesa> obtenerTodas();
    List<Mesa> obtenerPorEstado(EstadoMesa estado);
    Mesa obtenerPorId(Long id);
    Mesa crear(Mesa mesa);
    Mesa actualizar(Long id, Mesa mesaActualizada);
    Mesa cambiarEstado(Long id, EstadoMesa nuevoEstado);
    // Usados por el servicio de cuentas: abrir una cuenta ocupa la mesa y pagarla la libera
    Mesa ocupar(Long id, Long idCuenta);
    Mesa liberar(Long id);
    void eliminar(Long id);
}
