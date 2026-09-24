package com.example.despacho.controller;

import com.example.despacho.dto.CrearDespachoRequest;
import com.example.despacho.dto.EventoDespacho;
import com.example.despacho.model.Despacho;
import com.example.despacho.service.DespachoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/despachos")
public class DespachoController {

    private final DespachoService service;

    public DespachoController(DespachoService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<Despacho>> crear(
            @Valid @RequestBody CrearDespachoRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return service.crear(request, idempotencyKey)
                .map(d -> ResponseEntity.status(HttpStatus.CREATED).body(d));
    }

    @GetMapping("/{id}")
    public Mono<Despacho> obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping("/{id}/confirm")
    public Mono<Despacho> confirmar(@PathVariable Long id) {
        return service.confirmar(id);
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<EventoDespacho>> eventos(@PathVariable Long id) {
        return service.eventos(id)
                .map(e -> ServerSentEvent.builder(e)
                        .event(e.estado() == null ? "heartbeat" : "despacho")
                        .build());
    }
}
