package com.example.despacho.common;

/**
 * Marca errores transitorios (timeouts, 5xx del simulador externo) para que
 * TransportistaClient los reintente con retryWhen(Retry.backoff(...)).filter(...).
 * Los errores de dominio (4xx) NUNCA se envuelven en esta clase, así que el
 * filtro del retry los deja pasar sin reintentar (ver T3 - Destacado).
 */
public class TransientException extends RuntimeException {
    public TransientException(String message) {
        super(message);
    }

    public TransientException(String message, Throwable cause) {
        super(message, cause);
    }
}