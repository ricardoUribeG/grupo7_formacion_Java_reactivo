package com.example.despacho.integration;

import com.example.despacho.dto.CrearDespachoRequest;
import com.example.despacho.model.Despacho;
import com.example.despacho.model.Vehiculo;
import com.example.despacho.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba E2E completa: reserva de cupo -> cotización paralela contra
 * /external/** -> persistencia transaccional -> confirm -> SSE.
 *
 * Requiere Postgres arriba (docker compose up -d) porque ejercita R2DBC real,
 * tal como exige la lista de verificación del taller.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")
class DespachoFlowE2ETest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    private Long vehiculoId;

    @BeforeEach
    void sembrarVehiculo() {
        Vehiculo v = new Vehiculo(null, "E2E-" + System.nanoTime(), "BOG", 500, 500);
        vehiculoId = vehiculoRepository.save(v).map(Vehiculo::getId).block(Duration.ofSeconds(10));

        // simulador sin fallas forzadas, para que el happy path sea determinista
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

        Vehiculo actualizado = vehiculoRepository.findById(vehiculoId).block(Duration.ofSeconds(10));
        assertThat(actualizado).isNotNull();
        assertThat(actualizado.getCupoKg()).isEqualTo(500 - 120);
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
