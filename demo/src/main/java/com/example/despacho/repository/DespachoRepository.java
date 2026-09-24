package com.example.despacho.repository;

import com.example.despacho.model.Despacho;
import com.example.despacho.model.EstadoDespacho;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface DespachoRepository extends ReactiveCrudRepository<Despacho, Long> {
    Mono<Despacho> findByIdempotencyKey(String idempotencyKey);

    Flux<Despacho> findByEstadoAndExpiraEnBefore(EstadoDespacho estado, Instant antesDe);
}
