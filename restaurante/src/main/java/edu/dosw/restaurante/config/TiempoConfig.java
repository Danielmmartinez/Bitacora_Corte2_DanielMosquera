package edu.dosw.restaurante.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * El reloj se inyecta en vez de llamar a LocalDateTime.now() directamente: así las pruebas
 * pueden usar un reloj fijo (Clock.fixed) y comprobar cobros o fechas sin esperar horas reales.
 */
@Configuration
@EnableConfigurationProperties({ReservaProperties.class, ParqueaderoProperties.class})
public class TiempoConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
