package com.example.despacho.repository;

import com.example.despacho.model.Paquete;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PaqueteRepository extends ReactiveCrudRepository<Paquete, Long> {
    Flux<Paquete> findByDespachoId(Long despachoId);
}
