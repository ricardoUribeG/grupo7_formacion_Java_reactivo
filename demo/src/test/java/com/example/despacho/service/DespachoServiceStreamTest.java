package com.example.despacho.service;

import com.example.despacho.common.AppProperties;
import com.example.despacho.dto.EventoDespacho;
import com.example.despacho.model.Despacho;
import com.example.despacho.model.EstadoDespacho;
import com.example.despacho.repository.DespachoRepository;
import com.example.despacho.repository.PaqueteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DespachoServiceStreamTest {

    @Test
    void cancelarSse_cancelaLaSuscripcionAlBus() {
        DespachoRepository despachos = mock(DespachoRepository.class);
        PaqueteRepository paquetes = mock(PaqueteRepository.class);
        EventBus bus = mock(EventBus.class);
        TestPublisher<EventoDespacho> eventos = TestPublisher.create();

        Despacho despacho = new Despacho();
        despacho.setId(1L);
        despacho.setEstado(EstadoDespacho.ASIGNADO);
        despacho.setTrazaId("traza-stream");

        when(despachos.findById(1L)).thenReturn(Mono.just(despacho));
        when(paquetes.findByDespachoId(1L)).thenReturn(Flux.empty());
        when(bus.eventosDespacho()).thenReturn(eventos.flux());

        DespachoService servicio = new DespachoService(
                despachos,
                paquetes,
                mock(AsignacionSaga.class),
                mock(TransportistaClient.class),
                bus,
                mock(AppProperties.class),
                mock(TransactionalOperator.class));
        EventoDespacho eventoVivo = EventoDespacho.de(
                1L, EstadoDespacho.RECIBIDO, "actualización", "traza-stream");

        StepVerifier.create(servicio.eventos(1L))
                .expectNextMatches(evento -> evento.despachoId().equals(1L)
                        && evento.estado() == EstadoDespacho.ASIGNADO)
                .then(() -> eventos.next(eventoVivo))
                .expectNext(eventoVivo)
                .thenCancel()
                .verify();

        eventos.assertCancelled();
    }
}
