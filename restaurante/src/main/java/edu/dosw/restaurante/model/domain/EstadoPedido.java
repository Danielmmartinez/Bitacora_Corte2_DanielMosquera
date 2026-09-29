package edu.dosw.restaurante.model.domain;

public enum EstadoPedido {
    RECIBIDO,
    EN_PREPARACION,
    LISTO,
    ENTREGADO,
    CANCELADO;

    public boolean puedeTransicionarA(EstadoPedido siguiente) {
        return switch (this) {
            case RECIBIDO -> siguiente == EN_PREPARACION || siguiente == CANCELADO;
            case EN_PREPARACION -> siguiente == LISTO;
            case LISTO -> siguiente == ENTREGADO;
            default -> false;
        };
    }

    public boolean esCancelable() {
        return this == RECIBIDO;
    }

    /** ENTREGADO o CANCELADO: la cocina ya no tiene nada que hacer con el pedido. */
    public boolean esFinal() {
        return this == ENTREGADO || this == CANCELADO;
    }
}
