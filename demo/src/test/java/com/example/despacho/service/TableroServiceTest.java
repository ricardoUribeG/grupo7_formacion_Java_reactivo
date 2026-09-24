package com.example.despacho.service;

import com.example.despacho.dto.EventoTablero;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TableroServiceTest {

    @Test
    void clienteLento_recibeElEventoMasReciente() {
        TestPublisher<EventoTablero> fuente = TestPublisher.create();
        EventBus bus = mock(EventBus.class);
        when(bus.eventosTablero()).thenReturn(fuente.flux());

        TableroService servicio = new TableroService(bus);
        EventoTablero primero = new EventoTablero("despacho", "primero", Instant.parse("2026-01-01T00:00:00Z"));
        EventoTablero ultimo = new EventoTablero("despacho", "ultimo", Instant.parse("2026-01-01T00:00:01Z"));

        StepVerifier.create(servicio.tablero(), 0)
                .then(() -> fuente.next(primero, ultimo))
                .thenRequest(1)
                .expectNext(ultimo)
                .thenCancel()
                .verify();
    }
}
