package edu.dosw.restaurante.model.domain;

public enum EstadoReserva {
    CONFIRMADA,   // vigente: bloquea la mesa en su horario
    CANCELADA,    // la canceló el cliente o el restaurante
    CUMPLIDA      // el cliente llegó y se le abrió la cuenta
}
