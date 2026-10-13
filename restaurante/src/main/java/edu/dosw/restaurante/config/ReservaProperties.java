package edu.dosw.restaurante.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.LocalTime;

/**
 * Reglas de reservas configurables desde application.properties (prefijo restaurante.reservas).
 * Si una propiedad no está definida, se usa el @DefaultValue.
 */
@ConfigurationProperties(prefix = "restaurante.reservas")
public record ReservaProperties(
        @DefaultValue("12:00") LocalTime horaApertura,
        @DefaultValue("21:00") LocalTime horaUltimaReserva,
        @DefaultValue("120") int duracionMinutos) {
}
