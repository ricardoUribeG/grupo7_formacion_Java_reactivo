package com.example.despacho.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;


@Repository
public interface ProductReactiveRepository extends ReactiveCrudRepository<Producto, Long> {
    Flux<Producto> findByStockLessThan(int threshold);
}
