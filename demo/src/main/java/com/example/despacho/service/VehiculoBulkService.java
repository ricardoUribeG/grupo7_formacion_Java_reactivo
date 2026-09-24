package com.example.despacho.service;

import com.example.despacho.dto.ResultadoCarga;
import com.example.despacho.dto.VehiculoBulkItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** OE9 (parte de carga masiva): NDJSON de entrada, lotes de 500, upsert por id. */
@Service
public class VehiculoBulkService {

    private static final Logger log = LoggerFactory.getLogger(VehiculoBulkService.class);
    private static final int TAMANO_LOTE = 500;

    private final DatabaseClient client;

    public VehiculoBulkService(DatabaseClient client) {
        this.client = client;
    }

    public Mono<ResultadoCarga> cargar(Flux<VehiculoBulkItem> items) {
        return items
                .buffer(TAMANO_LOTE)
                .concatMap(lote -> Flux.fromIterable(lote)
                        .flatMap(this::upsert, 16)
                        .reduce(ResultadoCarga.vacio(), ResultadoCarga::mas))
                .reduce(ResultadoCarga.vacio(), ResultadoCarga::mas);
    }

    private Mono<ResultadoCarga> upsert(VehiculoBulkItem item) {
        if (item.id() == null || item.placa() == null || item.ciudad() == null || item.cupoKg() == null) {
            log.warn("Registro de vehículo inválido en carga masiva: {}", item);
            return Mono.just(new ResultadoCarga(0, 1));
        }
        return client.sql("""
                        INSERT INTO vehiculo (id, placa, ciudad, cupo_kg, cupo_kg_original, actualizado_en)
                        VALUES (:id, :placa, :ciudad, :cupo, :cupo, now())
                        ON CONFLICT (id) DO UPDATE
                            SET placa = EXCLUDED.placa,
                                ciudad = EXCLUDED.ciudad,
                                cupo_kg = EXCLUDED.cupo_kg,
                                cupo_kg_original = EXCLUDED.cupo_kg_original,
                                actualizado_en = now()
                        """)
                .bind("id", item.id())
                .bind("placa", item.placa())
                .bind("ciudad", item.ciudad())
                .bind("cupo", item.cupoKg())
                .fetch().rowsUpdated()
                .map(n -> new ResultadoCarga(1, 0))
                .onErrorResume(ex -> {
                    log.warn("No se pudo guardar vehículo {}: {}", item.id(), ex.toString());
                    return Mono.just(new ResultadoCarga(0, 1));
                });
    }
}
