package edu.dosw.restaurante.service;

import edu.dosw.restaurante.model.domain.reporte.PlatoVendido;
import edu.dosw.restaurante.model.domain.reporte.ReporteIngresos;
import edu.dosw.restaurante.model.domain.reporte.ResumenDia;

import java.time.LocalDate;
import java.util.List;

/**
 * Las fechas null significan "hoy".
 */
public interface IReporteService {
    ResumenDia resumenDelDia(LocalDate fecha);
    List<PlatoVendido> platosMasVendidos(LocalDate desde, LocalDate hasta, int limite);
    ReporteIngresos ingresos(LocalDate desde, LocalDate hasta);
}
