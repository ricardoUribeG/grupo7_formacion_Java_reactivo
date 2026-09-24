package com.example.despacho.service;

import com.example.despacho.common.AppProperties;
import com.example.despacho.common.TransientException;
import com.example.despacho.dto.ScoreZona;
import com.example.despacho.dto.TarifaCiudad;
import com.example.despacho.dto.VentanaClima;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OE4 (resiliencia), OE5 (cold vs hot / cache) y OE6 (backpressure lo maneja
 * ReporteService, no este cliente).
 *
 * Tres estrategias distintas, una por servicio externo simulado, cada una
 * justificada por el comportamiento del servicio que envuelve:
 *
 * - tarifa(ciudad): el simulador falla intermitentemente -> reintento con
 *   backoff exponencial + jitter, filtrando solo TransientException (nunca
 *   reintenta 4xx de dominio), y si se agotan los reintentos cae a la tarifa
 *   base de catálogo (fallback) en vez de romper el flujo completo.
 * - clima(ciudad): el simulador es simplemente lento, no falla -> no tiene
 *   sentido reintentar (empeoraría la latencia), así que el resultado se
 *   cachea 10 minutos por ciudad con Mono.cache(Duration) (frío -> se
 *   recalcula cada 10 min; entre medio es "caliente" para todos los
 *   suscriptores que piden esa ciudad).
 * - riesgoZona(ciudad): el simulador puede colgarse indefinidamente -> se le
 *   pone un timeout corto y, si se cumple, se usa un score por defecto
 *   conservador en vez de bloquear la creación del despacho.
 */
@Service
public class TransportistaClient {

    private static final Logger log = LoggerFactory.getLogger(TransportistaClient.class);

    private final WebClient webClient;
    private final AppProperties props;
    private final Map<String, Mono<VentanaClima>> climaCache = new ConcurrentHashMap<>();

    public TransportistaClient(WebClient transportistaWebClient, AppProperties props) {
        this.webClient = transportistaWebClient;
        this.props = props;
    }

    public Mono<TarifaCiudad> tarifa(String ciudad) {
        Mono<TarifaCiudad> llamada = webClient.get()
                .uri("/external/tarifa/{ciudad}", ciudad)
                .retrieve()
                .onStatus(s -> s.is5xxServerError(), r -> r.createException()
                        .flatMap(e -> Mono.error(new TransientException("tarifa transitoria: " + e.getMessage(), e))))
                .bodyToMono(TarifaCiudad.class)
                .timeout(props.external().tarifaTimeout());

        return llamada
                .retryWhen(Retry.backoff(3, Duration.ofMillis(200))
                        .jitter(0.3)
                        .filter(ex -> ex instanceof TransientException
                                || ex instanceof java.util.concurrent.TimeoutException)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .onErrorResume(ex -> {
                    log.warn("Tarifa dinámica no disponible para {} ({}); fallback a catálogo",
                            ciudad, ex.toString());
                    return Mono.just(new TarifaCiudad(ciudad, props.tarifaBasePorKg(), "catalogo-fallback"));
                });
    }

    public Mono<VentanaClima> clima(String ciudad) {
        return climaCache.computeIfAbsent(ciudad, c -> webClient.get()
                .uri("/external/clima/{ciudad}", c)
                .retrieve()
                .bodyToMono(VentanaClima.class)
                .timeout(props.external().climaTimeout())
                .cache(Duration.ofMinutes(10)));
    }

    public Mono<Integer> riesgoZona(String ciudad) {
        return webClient.get()
                .uri("/external/riesgo/{ciudad}", ciudad)
                .retrieve()
                .bodyToMono(ScoreZona.class)
                .timeout(props.external().riesgoTimeout())
                .map(ScoreZona::score)
                .onErrorResume(ex -> {
                    log.warn("Score de riesgo no disponible a tiempo para {} ({}); score por defecto {}",
                            ciudad, ex.toString(), props.defaultRiskScore());
                    return Mono.just(props.defaultRiskScore());
                });
    }
}