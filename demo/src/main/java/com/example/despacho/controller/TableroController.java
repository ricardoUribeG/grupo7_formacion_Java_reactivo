package com.example.despacho.controller;

import com.example.despacho.dto.EventoTablero;
import com.example.despacho.service.TableroService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ops")
public class TableroController {

    private final TableroService tableroService;

    public TableroController(TableroService tableroService) {
        this.tableroService = tableroService;
    }

    @GetMapping(value = "/tablero", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<EventoTablero>> tablero() {
        return tableroService.tablero()
                .map(e -> ServerSentEvent.builder(e).event(e.tipo()).build());
    }
}