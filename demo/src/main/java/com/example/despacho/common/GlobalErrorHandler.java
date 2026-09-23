package com.example.despacho.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OE11: manejo de errores de dominio de forma reactiva y centralizada.
 * Cuerpo de error uniforme exigido por el enunciado:
 * { "codigo", "mensaje", "trazaId", "instante" }
 */
@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(DomainException.class)
    public Mono<ResponseEntity<Map<String, Object>>> dominio(DomainException ex) {
        return cuerpo(ex.status(), ex.codigo(), ex.getMessage());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> validacion(WebExchangeBindException ex) {
        String detalle = ex.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return cuerpo(HttpStatus.BAD_REQUEST, "VALIDACION", detalle);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<Map<String, Object>>> entradaInvalida(ServerWebInputException ex) {
        return cuerpo(HttpStatus.BAD_REQUEST, "VALIDACION", "Cuerpo de la petición inválido");
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> inesperado(Exception ex) {
        return cuerpo(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Error inesperado: " + ex.getMessage());
    }

    private Mono<ResponseEntity<Map<String, Object>>> cuerpo(HttpStatus status, String codigo, String mensaje) {
        return Mono.deferContextual(ctx -> {
            String trazaId = ctx.getOrDefault(TrazaWebFilter.CONTEXT_KEY, "sin-traza");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("codigo", codigo);
            body.put("mensaje", mensaje);
            body.put("trazaId", trazaId);
            body.put("instante", Instant.now().toString());
            return Mono.just(ResponseEntity.status(status).body(body));
        });
    }
}