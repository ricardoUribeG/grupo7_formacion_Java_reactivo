package com.example.despacho.dto;

import com.example.despacho.model.EstadoOrden;

import java.time.Instant;

public record EventoOrden(Long ordenId, EstadoOrden estado, String mensaje, Instant timestamp) {
    public static EventoOrden de(Long ordenId, EstadoOrden estado, String mensaje) {
        return new EventoOrden(ordenId, estado, mensaje, Instant.now());
    }
    public static EventoOrden heartbeat(Long ordenId) {
        return new EventoOrden(ordenId, null, "heartbeat", Instant.now());
    }
}
