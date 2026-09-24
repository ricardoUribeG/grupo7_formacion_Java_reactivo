package com.example.despacho.dto;

/**
 * Configuración mutable del simulador de servicios externos, expuesta por
 * GET/PUT/DELETE /external/simulator.
 *
 * fallasTarifa: cuántas veces seguidas debe fallar /external/tarifa/{ciudad} antes de responder ok.
 * latenciaClimaMs: retardo artificial de /external/clima/{ciudad} (para forzar el cache).
 * latenciaRiesgoMs: retardo artificial de /external/riesgo/{ciudad} (para forzar el timeout).
 * scoreRiesgoForzado: si no es null, /external/riesgo/{ciudad} siempre responde ese score.
 */
public record SimuladorConfig(int fallasTarifa, long latenciaClimaMs, long latenciaRiesgoMs, Integer scoreRiesgoForzado) {

    public static SimuladorConfig porDefecto() {
        return new SimuladorConfig(0, 0, 0, null);
    }
}
