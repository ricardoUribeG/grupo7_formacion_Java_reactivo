package com.example.despacho.service;

import com.example.despacho.dto.EventoDespacho;
import com.example.despacho.dto.EventoTablero;
import com.example.despacho.model.Despacho;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * OE5 / T4: bus interno con dos Sinks distintos, cada uno justificado:
 *
 * - `despachos`: Sinks.many().replay().limit(200) porque un cliente que abre
 *   GET /api/despachos/{id}/events después de que el evento ya ocurrió
 *   todavía necesita ver el último estado (replay), no solo lo que pase desde
 *   que se suscribe.
 * - `tablero`: Sinks.many().multicast().onBackpressureBuffer() porque el
 *   tablero es "vivo": no queremos replay para nuevos operadores (verían
 *   historia vieja mezclada con el presente), solo lo que ocurre desde que
 *   están mirando la pantalla; ver TableroService para el publish().refCount()
 *   que comparte la MISMA suscripción entre todos los operadores conectados.
 */
@Component
public class EventBus {

    private final Sinks.Many<EventoDespacho> despachos = Sinks.many().replay().limit(200);
    private final Sinks.Many<EventoTablero> tablero = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Despacho> asignados = Sinks.many().multicast().onBackpressureBuffer();

    public void publicar(EventoDespacho evento) {
        despachos.emitNext(evento, Sinks.EmitFailureHandler.busyLooping(java.time.Duration.ofMillis(100)));
        tablero.emitNext(EventoTablero.de("despacho", evento), Sinks.EmitFailureHandler.busyLooping(java.time.Duration.ofMillis(100)));
    }

    public void publicarAsignado(Despacho despacho) {
        asignados.emitNext(despacho, Sinks.EmitFailureHandler.busyLooping(java.time.Duration.ofMillis(100)));
    }

    public Flux<EventoDespacho> eventosDespacho() { return despachos.asFlux(); }

    public Flux<EventoTablero> eventosTablero() { return tablero.asFlux(); }

    public Flux<Despacho> despachosAsignados() { return asignados.asFlux(); }
}
