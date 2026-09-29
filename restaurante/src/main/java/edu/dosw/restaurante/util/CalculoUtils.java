package edu.dosw.restaurante.util;

/**
 * Cálculos numéricos genéricos. Los double acumulan errores (0.1 + 0.2 = 0.30000000000000004),
 * así que los totales se redondean a 2 decimales antes de mostrarlos.
 */
public final class CalculoUtils {

    private CalculoUtils() {
    }

    public static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /** Promedio seguro: 0 si no hay elementos (evita dividir por cero). */
    public static double promedio(double total, long cantidad) {
        return cantidad == 0 ? 0.0 : redondear(total / cantidad);
    }
}
