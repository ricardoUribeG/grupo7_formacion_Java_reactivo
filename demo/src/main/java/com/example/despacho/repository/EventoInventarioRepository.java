package com.example.despacho.repository;

import com.example.despacho.model.EventoInventario;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface EventoInventarioRepository extends ReactiveCrudRepository<EventoInventario, Long> {
}
