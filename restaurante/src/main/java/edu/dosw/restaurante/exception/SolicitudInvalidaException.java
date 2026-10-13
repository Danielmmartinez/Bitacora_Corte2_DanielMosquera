package edu.dosw.restaurante.exception;

/**
 * Parámetros coherentes en formato pero sin sentido entre sí (ej. "desde" posterior a "hasta").
 * Se responde 400 igual que un @Valid fallido.
 */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String message) {
        super(message);
    }
}
