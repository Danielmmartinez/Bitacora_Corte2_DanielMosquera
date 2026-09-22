package edu.dosw.restaurante.exception;

import edu.dosw.restaurante.model.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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
        assertTrue(response.getBody().getMessage().contains("Error interno no esperado"));
    }
}