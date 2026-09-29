package edu.dosw.restaurante.config;

import edu.dosw.restaurante.security.JwtAuthFilter;
import edu.dosw.restaurante.security.RespuestasSeguridad;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Reglas globales de seguridad:
 *  - qué rutas son públicas y cuáles requieren token (aquí)
 *  - qué ROL puede usar cada endpoint (con @PreAuthorize en cada Controller)
 */
@Configuration
@EnableWebSecurity
// Activa @PreAuthorize. proxyTargetClass = true: los controllers implementan una interfaz (PlatoApi...);
// un proxy basado en esa interfaz perdería los @RequestMapping de la clase y las rutas darían 404.
@EnableMethodSecurity(proxyTargetClass = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RespuestasSeguridad respuestasSeguridad;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // JWT va en un header, no en una cookie: no hay riesgo de CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // Usa el bean CorsConfigurationSource de CorsConfig
                .cors(Customizer.withDefaults())
                // Sin sesiones en el servidor: cada petición se autentica con su token (stateless)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Públicas
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/registro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/menu/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Todo lo demás requiere token; el rol lo decide @PreAuthorize en cada endpoint
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respuestasSeguridad) // 401 en JSON
                        .accessDeniedHandler(respuestasSeguridad))     // 403 en JSON
                // La consola de H2 se muestra dentro de un <frame>
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                // Nuestro filtro valida el JWT antes del filtro estándar de usuario/contraseña
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // JwtAuthFilter es un @Component: sin esto, Spring Boot también lo registraría como filtro
    // general del servidor, además de dentro de la cadena de seguridad.
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> desactivarRegistroAutomatico(JwtAuthFilter filtro) {
        FilterRegistrationBean<JwtAuthFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Lo usa el login: verifica email + contraseña contra UsuarioDetailsService y PasswordEncoder
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
