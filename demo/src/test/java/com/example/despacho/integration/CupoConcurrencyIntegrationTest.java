package com.example.despacho.integration;

import com.example.despacho.common.DomainExceptions.CupoInsuficienteException;
import com.example.despacho.model.Vehiculo;
import com.example.despacho.repository.VehiculoRepository;
import com.example.despacho.service.CupoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CupoConcurrencyIntegrationTest {

    private final CupoService cupoService;
    private final VehiculoRepository vehiculos;
    private Long vehiculoId;

    @Autowired
    CupoConcurrencyIntegrationTest(CupoService cupoService, VehiculoRepository vehiculos) {
        this.cupoService = cupoService;
        this.vehiculos = vehiculos;
    }

    @BeforeEach
    void crearVehiculo() {
        String placa = "CON-" + UUID.randomUUID().toString().substring(0, 12);
        Vehiculo vehiculo = new Vehiculo(null, placa, "BOG", 500, 500);

        vehiculos.save(vehiculo)
                .map(Vehiculo::getId)
                .as(StepVerifier::create)
                .consumeNextWith(id -> vehiculoId = id)
                .verifyComplete();
    }

    @Test
    void reservasConcurrentes_nuncaDejanCupoNegativo() {
        Mono<Vehiculo> resultado = Flux.range(0, 20)
                .flatMap(i -> cupoService.reservar(vehiculoId, 30)
                        .onErrorResume(CupoInsuficienteException.class, error -> Mono.empty()), 20)
                .then(vehiculos.findById(vehiculoId));

        StepVerifier.create(resultado)
                .assertNext(vehiculo -> {
                    assertThat(vehiculo.getCupoKg()).isGreaterThanOrEqualTo(0);
                    assertThat(vehiculo.getCupoKg()).isEqualTo(20);
                })
                .verifyComplete();
    }
}
