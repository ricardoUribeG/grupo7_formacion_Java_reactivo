package com.example.despacho.service;

import com.example.despacho.common.AppProperties;
import com.example.despacho.dto.TarifaCiudad;
import com.example.despacho.dto.VentanaClima;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No se levanta ningún servidor real: se reemplaza el ExchangeFunction del
 * WebClient por un stub controlado en memoria, así se pueden probar
 * retry/backoff, timeout y cache sin red ni Mockito de por medio.
 */
class TransportistaClientTest {

    private static final AppProperties PROPS = new AppProperties(
            new AppProperties.External("http://simulado", Duration.ofMillis(1500), Duration.ofMillis(3000), Duration.ofMillis(200)),
            Duration.ofMinutes(15), Duration.ofSeconds(30), 80, 30, 3500);

    @Test
    void tarifa_reintentaFallosTransitoriosYLuegoResponde() {
        AtomicInteger llamadas = new AtomicInteger();
        WebClient client = WebClient.builder()
                .exchangeFunction(req -> {
                    int n = llamadas.incrementAndGet();
                    if (n < 3) {
                        return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build());
                    }
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .body("{\"ciudad\":\"BOG\",\"tarifaPorKg\":4000.0,\"fuente\":\"simulador\"}")
                            .build());
                })
                .build();

        TransportistaClient transportista = new TransportistaClient(client, PROPS);

        StepVerifier.create(transportista.tarifa("BOG"))
                .assertNext(t -> assertThat(t.tarifaPorKg()).isEqualTo(4000.0))
                .verifyComplete();

        assertThat(llamadas.get()).isEqualTo(3);
    }

    @Test
    void tarifa_siAgotaReintentos_caeAlFallbackDeCatalogo() {
        WebClient client = WebClient.builder()
                .exchangeFunction(req -> Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build()))
                .build();

        TransportistaClient transportista = new TransportistaClient(client, PROPS);

        StepVerifier.create(transportista.tarifa("MDE"))
                .assertNext(t -> {
                    assertThat(t.fuente()).isEqualTo("catalogo-fallback");
                    assertThat(t.tarifaPorKg()).isEqualTo(PROPS.tarifaBasePorKg());
                })
                .verifyComplete();
    }

    @Test
    void tarifa_conError4xx_noReintentaYCaeDirectoAlFallback() {
        AtomicInteger llamadas = new AtomicInteger();
        WebClient client = WebClient.builder()
                .exchangeFunction(req -> {
                    llamadas.incrementAndGet();
                    return Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).build());
                })
                .build();

        TransportistaClient transportista = new TransportistaClient(client, PROPS);

        StepVerifier.create(transportista.tarifa("BOG"))
                .assertNext(t -> assertThat(t.fuente()).isEqualTo("catalogo-fallback"))
                .verifyComplete();

        // un 4xx es un error de dominio, no transitorio: no se reintenta
        assertThat(llamadas.get()).isEqualTo(1);
    }

    @Test
    void riesgoZona_siElExternoSeCuelga_usaElScorePorDefectoTrasElTimeout() {
        WebClient client = WebClient.builder()
                .exchangeFunction(req -> Mono.just(ClientResponse.create(HttpStatus.OK)
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body("{\"ciudad\":\"BOG\",\"score\":10}")
                                .build())
                        .delayElement(Duration.ofSeconds(2))) // > riesgoTimeout (200ms) del PROPS de prueba
                .build();

        TransportistaClient transportista = new TransportistaClient(client, PROPS);

        StepVerifier.withVirtualTime(() -> transportista.riesgoZona("BOG"))
                .thenAwait(PROPS.external().riesgoTimeout())
                .assertNext(score -> assertThat(score).isEqualTo(PROPS.defaultRiskScore()))
                .verifyComplete();
    }

    @Test
    void clima_conFuenteControlada_seCacheaParaLaMismaCiudad() {
        AtomicInteger llamadas = new AtomicInteger();
        TestPublisher<ClientResponse> respuestas = TestPublisher.createCold();
        WebClient client = WebClient.builder()
                .exchangeFunction(req -> {
                    llamadas.incrementAndGet();
                    return respuestas.mono();
                })
                .build();

        TransportistaClient transportista = new TransportistaClient(client, PROPS);

        Mono<VentanaClima> primera = transportista.clima("BOG");
        Mono<VentanaClima> segunda = transportista.clima("BOG");

        StepVerifier.create(primera)
                .then(() -> respuestas.emit(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"ciudad\":\"BOG\",\"minutosEntrega\":25}")
                        .build()))
                .assertNext(ventana -> assertThat(ventana.minutosEntrega()).isEqualTo(25))
                .verifyComplete();
        StepVerifier.create(segunda).expectNextCount(1).verifyComplete();

        assertThat(llamadas.get()).isEqualTo(1);
    }
}