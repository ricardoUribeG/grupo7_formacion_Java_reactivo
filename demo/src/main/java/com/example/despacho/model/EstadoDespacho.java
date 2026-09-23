package com.example.despacho.model;

public enum EstadoDespacho {
    RECIBIDO, ASIGNADO, EN_RUTA, RECHAZADO, EXPIRADO, COMPENSADO;

    public boolean esTerminado() {
        return this == EN_RUTA || this == RECHAZADO || this == EXPIRADO || this == COMPENSADO;
    }
}