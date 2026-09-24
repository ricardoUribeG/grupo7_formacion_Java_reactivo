package com.example.despacho.repository;

import com.example.despacho.model.Vehiculo;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface VehiculoRepository extends ReactiveCrudRepository<Vehiculo, Long> {
    Flux<Vehiculo> findByCiudad(String ciudad);
}
