package com.example.despacho.service;

import com.example.despacho.common.DomainExceptions.CupoInsuficienteException;
import com.example.despacho.common.DomainExceptions.VehiculoNoExisteException;
import com.example.despacho.common.ReactiveSupport;
import com.example.despacho.model.Vehiculo;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * T5 (Persistencia reactiva, atomicidad y saga): el descuento/reintegro de
 * cupo se hace con UPDATE ... WHERE cupo_kg >= :peso RETURNING *, ejecutado
 * como una sola sentencia atómica por fila. Esto evita la carrera
 * lee-modifica-escribe: si dos despachos concurrentes pelean por el mismo
 * vehículo, Postgres serializa las dos UPDATE y la segunda simplemente ve el
 * cupo ya descontado por la primera (la condición WHERE decide, no una
 * lectura previa en el heap de Java).
 */
@Service
public class CupoService {

    private final DatabaseClient client;

    public CupoService(DatabaseClient client) {
        this.client = client;
    }

    /** Reserva peso_kg del vehículo si hay cupo suficiente; si no, error 409. */
    public Mono<Vehiculo> reservar(Long vehiculoId, double pesoKg) {
        int pesoRedondeado = (int) Math.ceil(pesoKg);
        Mono<Vehiculo> operacion = existe(vehiculoId)
                .then(client.sql("""
                        UPDATE vehiculo SET cupo_kg = cupo_kg - :peso, actualizado_en = now()
                        WHERE id = :id AND cupo_kg >= :peso
                        RETURNING id, placa, ciudad, cupo_kg, cupo_kg_original, actualizado_en
                        """)
                        .bind("peso", pesoRedondeado)
                        .bind("id", vehiculoId)
                        .map(this::mapear)
                        .one())
                .switchIfEmpty(Mono.error(new CupoInsuficienteException(vehiculoId, pesoKg)));

        return ReactiveSupport.traced("cupo.reservar#" + vehiculoId, operacion);
    }

    /** Compensación: devuelve peso_kg al vehículo (nunca falla la saga si esto falla; se registra). */
    public Mono<Vehiculo> liberar(Long vehiculoId, double pesoKg) {
        Mono<Vehiculo> operacion = client.sql("""
                        UPDATE vehiculo SET cupo_kg = LEAST(cupo_kg + :peso, cupo_kg_original), actualizado_en = now()
                        WHERE id = :id
                        RETURNING id, placa, ciudad, cupo_kg, cupo_kg_original, actualizado_en
                        """)
                .bind("peso", (int) Math.ceil(pesoKg))
                .bind("id", vehiculoId)
                .map(this::mapear)
                .one();

        return ReactiveSupport.traced("cupo.liberar#" + vehiculoId, operacion);
    }

    private Mono<Void> existe(Long vehiculoId) {
        return client.sql("SELECT id FROM vehiculo WHERE id = :id")
                .bind("id", vehiculoId)
                .fetch().first()
                .switchIfEmpty(Mono.error(new VehiculoNoExisteException(vehiculoId)))
                .then();
    }

    private Vehiculo mapear(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata meta) {
        Vehiculo v = new Vehiculo();
        v.setId(row.get("id", Long.class));
        v.setPlaca(row.get("placa", String.class));
        v.setCiudad(row.get("ciudad", String.class));
        v.setCupoKg(row.get("cupo_kg", Integer.class));
        v.setCupoKgOriginal(row.get("cupo_kg_original", Integer.class));
        return v;
    }
}
