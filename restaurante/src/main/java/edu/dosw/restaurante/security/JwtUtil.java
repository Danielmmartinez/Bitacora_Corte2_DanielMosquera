package edu.dosw.restaurante.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * Crea y valida los tokens JWT.
 * Un JWT tiene 3 partes separadas por puntos: header.payload.firma
 * La firma se calcula con la clave secreta: si alguien cambia el payload (por ejemplo, su rol),
 * la firma deja de coincidir y el token se rechaza.
 */
@Component
public class JwtUtil {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtUtil(@Value("${jwt.secret}") String secretoBase64,
                   @Value("${jwt.expiration}") long expiracionMs) {
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(UserDetails usuario) {
        List<String> roles = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        Date ahora = new Date();

        return Jwts.builder()
                .subject(usuario.getUsername())                       // "sub": el email
                .claim("roles", roles)                                // "roles": ["ROLE_GERENTE"]
                .issuedAt(ahora)                                      // "iat": cuándo se emitió
                .expiration(new Date(ahora.getTime() + expiracionMs)) // "exp": cuándo vence
                .signWith(clave)                                      // firma HMAC-SHA
                .compact();
    }

    public String extraerEmail(String token) {
        return parsear(token).getSubject();
    }

    /** true si la firma es correcta y no ha vencido. */
    public boolean esValido(String token) {
        try {
            parsear(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpiracionSegundos() {
        return expiracionMs / 1000;
    }

    private Claims parsear(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }
}
