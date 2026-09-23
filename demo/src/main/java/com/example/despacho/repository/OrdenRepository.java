package com.example.despacho.repository;

import com.example.despacho.model.Orden;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OrdenRepository extends ReactiveCrudRepository<Orden, Long> {
    Flux<Orden> findByClienteId(Long clienteId);
}
