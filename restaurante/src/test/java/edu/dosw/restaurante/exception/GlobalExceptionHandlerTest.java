package edu.dosw.restaurante.exception;

import edu.dosw.restaurante.model.domain.EstadoPedido;
import edu.dosw.restaurante.model.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void handleRecursoNoEncontrado_Devuelve404() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("No encontrado");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleRecursoNoEncontrado(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("No encontrado", response.getBody().getMessage());
    }

    @Test
    void handleConflicto_Devuelve409() {
        ConflictoException ex = new ConflictoException("Conflicto detectado");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleConflicto(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflicto detectado", response.getBody().getMessage());
    }

    @Test
    void handleEstadoInvalido_Devuelve422() {
        EstadoInvalidoException ex = new EstadoInvalidoException("Estado no valido");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleEstadoInvalido(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("Estado no valido", response.getBody().getMessage());
    }

    @Test
    void handleValidationExceptions_Devuelve400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("objectName", "nombre", "El nombre no puede estar vacío");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationExceptions(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody().getErrors());
        assertEquals("El nombre no puede estar vacío", response.getBody().getErrors().get("nombre"));
    }

    @Test
    void handleGlobalException_Devuelve500() {
        Exception ex = new Exception("Error inesperado");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleGlobalException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error interno no esperado", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("Error inesperado"));
    }

    @Test
    void handleTypeMismatch_Devuelve400() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "COCINADO", EstadoPedido.class, "estado", null, null);
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("estado"));
    }

    @Test
    void handleMissingParameter_Devuelve400() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("estado", "EstadoMesa");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleMissingParameter(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("estado"));
    }

    @Test
    void handleMessageNotReadable_Devuelve400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON mal formado",
                mock(HttpInputMessage.class));
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleMessageNotReadable(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleMethodNotSupported_Devuelve405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("PATCH");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }

    @Test
    void handleNoResourceFound_Devuelve404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "api/v1/inexistente");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleNoResourceFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleAutenticacion_Devuelve401() {
        ResponseEntity<ErrorResponseDTO> response =
                exceptionHandler.handleAutenticacion(new BadCredentialsException("Bad credentials"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Credenciales inválidas", response.getBody().getMessage());
    }

    @Test
    void handleAccesoDenegado_Devuelve403() {
        ResponseEntity<ErrorResponseDTO> response =
                exceptionHandler.handleAccesoDenegado(new AccessDeniedException("Access Denied"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void handleDataIntegrity_Devuelve409() {
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleDataIntegrity(
                new DataIntegrityViolationException("Unique index violation"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleBaseDeDatosNoDisponible_Devuelve503() {
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleBaseDeDatosNoDisponible(
                new DataAccessResourceFailureException("Timed out"), request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }
}
