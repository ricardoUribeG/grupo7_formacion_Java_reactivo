package com.example.despacho.integration;

import com.example.despacho.dto.CrearDespachoRequest;
import com.example.despacho.model.Despacho;
import com.example.despacho.model.Vehiculo;
import com.example.despacho.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
class DespachoFlowE2ETest {

    // 1. Declaramos las dependencias como final (Inmutabilidad)
    private final WebTestClient client;
    private final VehiculoRepository vehiculoRepository;

    private Long vehiculoId;

    // 2. JUnit 5 y Spring inyectan automáticamente los parámetros del constructor
    DespachoFlowE2ETest(WebTestClient client, VehiculoRepository vehiculoRepository) {
        this.client = client;
        this.vehiculoRepository = vehiculoRepository;
    }

    @BeforeEach
    void sembrarVehiculo() {
        Vehiculo v = new Vehiculo(null, "E2E-" + System.nanoTime(), "BOG", 500, 500);

        // Uso de StepVerifier para extraer el ID reactivamente sin bloquear (.block())
        vehiculoRepository.save(v)
                .map(Vehiculo::getId)
                .as(StepVerifier::create)
                .consumeNextWith(id -> this.vehiculoId = id)
                .verifyComplete();

        // Simulador sin fallas forzadas
        client.delete().uri("/external/simulator").exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("crear -> obtener -> confirmar: el despacho llega a EN_RUTA y el cupo del vehículo baja")
    void flujoCompleto_terminaEnRutaYDescuentaCupo() {
        var body = new CrearDespachoRequest(1L, "BOG", List.of(
                new CrearDespachoRequest.PaqueteItem(vehiculoId, 120.0)));

        Despacho creado = client.post().uri("/api/despachos")
                .header("X-Traza-Id", "e2e-1")
                .header("Idempotency-Key", "e2e-key-" + System.nanoTime())
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Despacho.class)
                .returnResult()
                .getResponseBody();

        assertThat(creado).isNotNull();
        assertThat(creado.getEstado().name()).isIn("ASIGNADO");
        assertThat(creado.getPaquetes()).hasSize(1);

        client.post().uri("/api/despachos/{id}/confirm", creado.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("EN_RUTA");

        // Verificación reactiva del repositorio usando StepVerifier
        vehiculoRepository.findById(vehiculoId)
                .as(StepVerifier::create)
                .assertNext(actualizado -> {
                    assertThat(actualizado).isNotNull();
                    assertThat(actualizado.getCupoKg()).isEqualTo(500 - 120);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("confirmar dos veces devuelve 409 (estado inválido)")
    void confirmarDosVeces_devuelve409() {
        var body = new CrearDespachoRequest(2L, "BOG", List.of(
                new CrearDespachoRequest.PaqueteItem(vehiculoId, 30.0)));

        Long id = client.post().uri("/api/despachos")
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Despacho.class)
                .returnResult()
                .getResponseBody()
                .getId();

        client.post().uri("/api/despachos/{id}/confirm", id).exchange().expectStatus().isOk();
        client.post().uri("/api/despachos/{id}/confirm", id).exchange().expectStatus().isEqualTo(409);
    }
}
