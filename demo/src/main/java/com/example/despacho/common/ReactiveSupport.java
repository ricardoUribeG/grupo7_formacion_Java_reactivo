package com.example.despacho.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * OE7 / T9 (utilidades reactivas reutilizables): centraliza el logging con
 * trazaId leído del Context, para no repetir doOnSubscribe/doOnError en cada
 * servicio.
 */
public final class ReactiveSupport {

    private static final Logger log = LoggerFactory.getLogger(ReactiveSupport.class);

    private ReactiveSupport() {}

    public static <T> Mono<T> traced(String paso, Mono<T> origen) {
        return Mono.deferContextual(ctx -> {
            String trazaId = ctx.getOrDefault(TrazaWebFilter.CONTEXT_KEY, "sin-traza");
            return origen
                    .doOnSubscribe(s -> log.info("[{}] {} -> inicio", trazaId, paso))
                    .doOnSuccess(v -> log.info("[{}] {} -> ok", trazaId, paso))
                    .doOnError(e -> log.warn("[{}] {} -> error {}", trazaId, paso, e.toString()))
                    .doFinally(sig -> log.debug("[{}] {} -> fin ({})", trazaId, paso, sig));
        });
    }

    public static <T> Flux<T> tracedFlux(String paso, Flux<T> origen) {
        return Flux.deferContextual(ctx -> {
            String trazaId = ctx.getOrDefault(TrazaWebFilter.CONTEXT_KEY, "sin-traza");
            return origen
                    .doOnSubscribe(s -> log.info("[{}] {} -> inicio", trazaId, paso))
                    .doOnComplete(() -> log.info("[{}] {} -> ok", trazaId, paso))
                    .doOnError(e -> log.warn("[{}] {} -> error {}", trazaId, paso, e.toString()))
                    .doFinally(sig -> log.debug("[{}] {} -> fin ({})", trazaId, paso, sig));
        });
    }
}
