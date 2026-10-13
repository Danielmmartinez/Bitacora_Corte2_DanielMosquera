package edu.dosw.restaurante.model.domain;

import java.util.Optional;

/**
 * El tipo se deduce del formato de la placa colombiana:
 * carro = 3 letras + 3 números (ABC123), moto = 3 letras + 2 números + 1 letra (ABC12D).
 */
public enum TipoVehiculo {
    CARRO("[A-Z]{3}[0-9]{3}"),
    MOTO("[A-Z]{3}[0-9]{2}[A-Z]");

    private final String formato;

    TipoVehiculo(String formato) {
        this.formato = formato;
    }

    /** Recibe la placa ya normalizada (mayúsculas, sin espacios ni guiones). */
    public static Optional<TipoVehiculo> desdePlaca(String placa) {
        if (placa == null) {
            return Optional.empty();
        }
        for (TipoVehiculo tipo : values()) {
            if (placa.matches(tipo.formato)) {
                return Optional.of(tipo);
            }
        }
        return Optional.empty();
    }
}
