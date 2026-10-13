package edu.dosw.restaurante.util;

import java.util.Locale;

/**
 * Utilidad de texto (sin lógica de negocio): deja las placas en un formato único
 * para que "abc-123", "ABC 123" y "ABC123" se consideren la misma placa.
 */
public final class PlacaUtils {

    private PlacaUtils() {
    }

    public static String normalizar(String placa) {
        if (placa == null) {
            return null;
        }
        return placa.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }
}
