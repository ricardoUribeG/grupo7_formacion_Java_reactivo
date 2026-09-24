package com.example.despacho.service;

import com.example.despacho.dto.EventoTablero;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * T4 (Destacado): el tablero global se expone como UN SOLO stream
 * compartido: publish().refCount(1) hace que la primera conexión dispare la
 * suscripción real al EventBus, y que las conexiones siguientes se sumen a
 * ESA MISMA suscripción (no crean una nueva ni reciben una copia
 * independiente) — dos `curl -N` al tablero ven exactamente los mismos
 * eventos, en el mismo orden, sin duplicar trabajo aguas arriba. Cuando el
 * último suscriptor se desconecta, refCount libera la suscripción interna.
 * onBackpressureLatest evita acumular eventos obsoletos si el consumidor no
 * puede seguir el ritmo: para un tablero importa el estado más reciente.
 */
@Service
public class TableroService {

    private final Flux<EventoTablero> compartido;

    public TableroService(EventBus bus) {
        this.compartido = bus.eventosTablero()
                .onBackpressureLatest()
                .publish()
                .refCount(1);
    }

    public Flux<EventoTablero> tablero() {
        return compartido;
    }
}
