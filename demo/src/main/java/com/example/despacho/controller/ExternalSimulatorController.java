package com.example.despacho.controller;

import com.example.despacho.common.AppProperties;
import com.example.despacho.dto.ScoreZona;
import com.example.despacho.dto.SimuladorConfig;
import com.example.despacho.dto.TarifaCiudad;
import com.example.despacho.dto.VentanaClima;
import com.example.despacho.service.SimuladorEstado;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulador de los tres servicios externos que consume TransportistaClient,
 * vivo dentro de esta misma app bajo /external/**, más el panel de control
 * /external/simulator que la lista de verificación del taller usa para
 * "romper" la demo (fallas intermitentes, latencia, riesgo forzado).
 */
@RestController
@RequestMapping("/external")
public class ExternalSimulatorController {

    private final SimuladorEstado estado;
    private final AppProperties props;

    public ExternalSimulatorController(SimuladorEstado estado, AppProperties props) {
        this.estado = estado;
        this.props = props;
    }

    @GetMapping("/tarifa/{ciudad}")
    public Mono<TarifaCiudad> tarifa(@PathVariable String ciudad) {
        if (estado.debeFallarTarifa()) {
            return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "tarifa simulada caída"));
        }
        double variacion = 0.9 + ThreadLocalRandom.current().nextDouble(0.2);
        return Mono.just(new TarifaCiudad(ciudad, Math.round(props.tarifaBasePorKg() * variacion * 100.0) / 100.0, "simulador"));
    }

    @GetMapping("/clima/{ciudad}")
    public Mono<VentanaClima> clima(@PathVariable String ciudad) {
        long latencia = estado.get().latenciaClimaMs();
        int minutos = 20 + ThreadLocalRandom.current().nextInt(40);
        Mono<VentanaClima> respuesta = Mono.just(new VentanaClima(ciudad, minutos));
        return latencia > 0 ? respuesta.delayElement(Duration.ofMillis(latencia)) : respuesta;
    }

    @GetMapping("/riesgo/{ciudad}")
    public Mono<ScoreZona> riesgo(@PathVariable String ciudad) {
        SimuladorConfig cfg = estado.get();
        int score = cfg.scoreRiesgoForzado() != null ? cfg.scoreRiesgoForzado() : ThreadLocalRandom.current().nextInt(0, 60);
        Mono<ScoreZona> respuesta = Mono.just(new ScoreZona(ciudad, score));
        return cfg.latenciaRiesgoMs() > 0 ? respuesta.delayElement(Duration.ofMillis(cfg.latenciaRiesgoMs())) : respuesta;
    }

    @GetMapping("/simulator")
    public Mono<SimuladorConfig> obtenerConfig() {
        return Mono.just(estado.get());
    }

    @PutMapping("/simulator")
    public Mono<SimuladorConfig> configurar(@RequestBody SimuladorConfig nueva) {
        estado.set(nueva);
        return Mono.just(estado.get());
    }

    @DeleteMapping("/simulator")
    public Mono<SimuladorConfig> reset() {
        estado.reset();
        return Mono.just(estado.get());
    }
}
