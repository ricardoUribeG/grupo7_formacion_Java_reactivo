package com.example.despacho.controller;

import com.example.despacho.dto.ReporteCiudad;
import com.example.despacho.service.ReporteService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/reports/ciudades")
public class ReporteController {

    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    @GetMapping
    public Flux<ReporteCiudad> totales() {
        return service.totalesPorCiudad();
    }

    @GetMapping(value = "/stream", produces = "application/x-ndjson")
    public Flux<ReporteCiudad> stream() {
        return service.acumuladoEnVivo();
    }
}
