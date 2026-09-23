package com.example.despacho.common;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * OE7: el trazaId viaja por Reactor Context (contextWrite/deferContextual),
 * nunca como parámetro de método. Este filtro es el único lugar que lee el
 * header X-Traza-Id; todo lo demás (servicios, logs, manejador de errores)
 * lo obtiene del Context.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrazaWebFilter implements WebFilter {

    public static final String CONTEXT_KEY = "trazaId";
    public static final String HEADER = "X-Traza-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String trazaId = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(HEADER))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());
        exchange.getResponse().getHeaders().add(HEADER, trazaId);
        return chain.filter(exchange).contextWrite(ctx -> ctx.put(CONTEXT_KEY, trazaId));
    }
}
