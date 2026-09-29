package edu.dosw.restaurante.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita @Async. Spring Boot aporta el pool de hilos (applicationTaskExecutor).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
