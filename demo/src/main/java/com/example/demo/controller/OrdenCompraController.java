package com.example.demo.controller;

import com.example.demo.dto.CrearOrdenRequest;
import com.example.demo.dto.EventoOrden;
import com.example.demo.model.OrdenCompra;
import com.example.demo.service.OrdenCompraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
public class OrdenCompraController {
    private final OrdenCompraService service;

    public OrdenCompraController(OrdenCompraService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<OrdenCompra>> crear(
            @RequestBody CrearOrdenRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return service.crear(request, idempotencyKey)
                .map(o -> ResponseEntity.status(HttpStatus.CREATED).body(o));
    }

    @GetMapping("/{id}")
    public Mono<OrdenCompra> obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping("/{id}/confirm")
    public Mono<OrdenCompra> confirmar(@PathVariable Long id) {
        return service.confirmar(id);
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<EventoOrden>> eventos(@PathVariable Long id) {
        return service.eventos(id)
                .map(e -> ServerSentEvent.builder(e)
                        .event(e.estado() == null ? "heartbeat" : "order")
                        .build());
    }
}
