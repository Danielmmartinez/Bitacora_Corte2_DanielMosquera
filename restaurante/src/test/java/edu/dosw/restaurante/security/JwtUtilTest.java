package edu.dosw.restaurante.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String CLAVE = Base64.getEncoder()
            .encodeToString("clave-de-prueba-de-al-menos-32-bytes!!".getBytes());
    private static final String OTRA_CLAVE = Base64.getEncoder()
            .encodeToString("otra-clave-distinta-de-al-menos-32-bytes".getBytes());

    private final JwtUtil jwtUtil = new JwtUtil(CLAVE, 60_000);
    private final UserDetails gerente = User.withUsername("gerente@sakura.com").password("x").roles("GERENTE").build();

    @Test
    void generarToken_TieneTresPartesYElEmail() {
        String token = jwtUtil.generarToken(gerente);

        assertEquals(3, token.split("\\.").length); // header.payload.firma
        assertTrue(jwtUtil.esValido(token));
        assertEquals("gerente@sakura.com", jwtUtil.extraerEmail(token));
        assertEquals(60, jwtUtil.getExpiracionSegundos());
    }

    @Test
    void payload_ContieneLosRoles() {
        String payload = new String(Base64.getUrlDecoder().decode(jwtUtil.generarToken(gerente).split("\\.")[1]));
        // El payload NO está cifrado, solo codificado: cualquiera puede leerlo. Por eso nunca va la contraseña.
        assertTrue(payload.contains("ROLE_GERENTE"));
        assertFalse(payload.contains("password"));
    }

    @Test
    void tokenVencido_NoEsValido() {
        JwtUtil expiraYa = new JwtUtil(CLAVE, -1000);
        assertFalse(jwtUtil.esValido(expiraYa.generarToken(gerente)));
    }

    @Test
    void tokenFirmadoConOtraClave_NoEsValido() {
        String ajeno = new JwtUtil(OTRA_CLAVE, 60_000).generarToken(gerente);
        assertFalse(jwtUtil.esValido(ajeno));
    }

    @Test
    void tokenAlterado_NoEsValido() {
        String[] partes = jwtUtil.generarToken(gerente).split("\\.");
        // Cambiar el payload (p. ej. para subirse de rol) invalida la firma
        String payloadFalso = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"cliente@sakura.com\",\"roles\":[\"ROLE_GERENTE\"]}".getBytes());
        assertFalse(jwtUtil.esValido(partes[0] + "." + payloadFalso + "." + partes[2]));
    }

    @Test
    void basura_NoEsValido() {
        assertFalse(jwtUtil.esValido("esto-no-es-un-jwt"));
        assertFalse(jwtUtil.esValido(""));
    }
}
