package com.example.despacho.dto;

import com.example.despacho.model.EstadoDespacho;

import java.time.Instant;

/**
 * T7 (Destacado): incluye el trazaId en cada evento SSE. Se toma del propio
 * Despacho (que lo capturó del Reactor Context una sola vez, al crearse en
 * DespachoService), en vez de volver a leer el header o pasarlo como
 * parámetro suelto por toda la cadena de llamadas.
 */
public record EventoDespacho(Long despachoId, EstadoDespacho estado, String mensaje, String trazaId, Instant timestamp) {

    public static EventoDespacho de(Long despachoId, EstadoDespacho estado, String mensaje, String trazaId) {
        return new EventoDespacho(despachoId, estado, mensaje, trazaId, Instant.now());
    }

    public static EventoDespacho heartbeat(Long despachoId, String trazaId) {
        return new EventoDespacho(despachoId, null, "heartbeat", trazaId, Instant.now());
    }
}