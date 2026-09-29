package edu.dosw.restaurante.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Se ejecuta en CADA petición, antes de llegar al Controller.
 * Si trae "Authorization: Bearer <token>" válido, registra al usuario como autenticado.
 * Si no trae token o es inválido, no hace nada: más adelante Spring Security responde 401
 * si la ruta lo requería.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIJO)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIJO.length());
        if (jwtUtil.esValido(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Se recarga el usuario para tomar su rol actual y rechazar usuarios eliminados
                UserDetails usuario = userDetailsService.loadUserByUsername(jwtUtil.extraerEmail(token));

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        usuario, null, usuario.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (UsernameNotFoundException e) {
                log.warn("Token válido de un usuario que ya no existe");
            }
        }

        chain.doFilter(request, response);
    }
}
