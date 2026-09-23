package com.example.despacho.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración tipada bajo el prefijo "app" (ver application.yml).
 * OE7: nada de esto se pasa como parámetro por la cadena de negocio;
 * se inyecta una sola vez en los beans que lo necesitan.
 */
@ConfigurationProperties("app")
public record AppProperties(
        External external,
        Duration reservationTtl,
        Duration expiryInterval,
        int riskThreshold,
        int defaultRiskScore,
        double tarifaBasePorKg) {

    public record External(String baseUrl, Duration tarifaTimeout, Duration climaTimeout, Duration riesgoTimeout) {}
}
