package edu.dosw.restaurante.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // Agrega el botón "Authorize" en Swagger UI: se pega el token y se envía en cada petición
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token obtenido en POST /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .info(new Info()
                        .title("Restaurante API - Bitácora 2")
                        .version("1.0.0")
                        .description("API RESTful para la gestión de platos, mesas, pedidos y cuentas de un restaurante. "
                                + "Inicia sesión en /api/v1/auth/login y usa el botón Authorize con el token.")
                        .contact(new Contact()
                                .name("Estudiante DOSW")
                                .email("estudiante@escuelaing.edu.co")));
    }
}