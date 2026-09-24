package com.example.despacho.service;

import com.example.despacho.dto.SimuladorConfig;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Estado compartido en memoria para /external/**. Permite romper la demo en
 * vivo con PUT /external/simulator (forzar fallas de tarifa, latencia de
 * clima/riesgo, score de riesgo fijo) y volver a la normalidad con DELETE.
 */
@Component
public class SimuladorEstado {

    private final AtomicReference<SimuladorConfig> config = new AtomicReference<>(SimuladorConfig.porDefecto());
    private final AtomicInteger fallasRestantes = new AtomicInteger(0);

    public SimuladorConfig get() {
        return config.get();
    }

    public void set(SimuladorConfig nueva) {
        config.set(nueva);
        fallasRestantes.set(nueva.fallasTarifa());
    }

    public void reset() {
        set(SimuladorConfig.porDefecto());
    }

    /** true si esta invocación de /external/tarifa debe fallar. */
    public boolean debeFallarTarifa() {
        return fallasRestantes.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0;
    }
}
