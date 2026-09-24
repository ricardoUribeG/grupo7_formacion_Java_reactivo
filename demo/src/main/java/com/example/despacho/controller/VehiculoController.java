package com.example.despacho.controller;

import com.example.despacho.dto.ResultadoCarga;
import com.example.despacho.dto.VehiculoBulkItem;
import com.example.despacho.model.Vehiculo;
import com.example.despacho.repository.VehiculoRepository;
import com.example.despacho.service.VehiculoBulkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoRepository repository;
    private final VehiculoBulkService bulkService;

    public VehiculoController(VehiculoRepository repository, VehiculoBulkService bulkService) {
        this.repository = repository;
        this.bulkService = bulkService;
    }

    @GetMapping
    public Flux<Vehiculo> todos() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Vehiculo>> porId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Vehiculo>> crear(@RequestBody Vehiculo vehiculo) {
        if (vehiculo.getCupoKgOriginal() == null) {
            vehiculo.setCupoKgOriginal(vehiculo.getCupoKg());
        }
        return repository.save(vehiculo).map(v -> ResponseEntity.status(HttpStatus.CREATED).body(v));
    }

    /** OE9: carga masiva NDJSON en lotes de 500 con upsert. */
    @PostMapping(value = "/bulk", consumes = "application/x-ndjson")
    public Mono<ResultadoCarga> bulk(@RequestBody Flux<VehiculoBulkItem> items) {
        return bulkService.cargar(items);
    }
}
