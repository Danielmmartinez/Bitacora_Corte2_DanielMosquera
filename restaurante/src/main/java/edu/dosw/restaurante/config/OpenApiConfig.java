package edu.dosw.restaurante.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Restaurante API - Bitácora 2")
                        .version("1.0.0")
                        .description("API RESTful para la gestión de platos, mesas y pedidos de un restaurante.")
                        .contact(new Contact()
                                .name("Estudiante DOSW")
                                .email("estudiante@escuelaing.edu.co")));
    }
}