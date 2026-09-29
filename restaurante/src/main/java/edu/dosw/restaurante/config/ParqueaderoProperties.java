package edu.dosw.restaurante.config;

import edu.dosw.restaurante.model.domain.TipoVehiculo;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del parqueadero (prefijo restaurante.parqueadero).
 */
@ConfigurationProperties(prefix = "restaurante.parqueadero")
public record ParqueaderoProperties(
        @DefaultValue("20") int capacidad,
        @DefaultValue("5000") double tarifaHoraCarro,
        @DefaultValue("2000") double tarifaHoraMoto) {

    public double tarifaPara(TipoVehiculo tipo) {
        return tipo == TipoVehiculo.MOTO ? tarifaHoraMoto : tarifaHoraCarro;
    }
}
