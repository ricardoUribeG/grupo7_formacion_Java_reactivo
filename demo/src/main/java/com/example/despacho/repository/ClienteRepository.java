package com.example.despacho.repository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ClienteRepository extends ReactiveCrudRepository<Cliente, Long> {
    Flux<Cliente> findByNombreContaining(String nombre);
}
