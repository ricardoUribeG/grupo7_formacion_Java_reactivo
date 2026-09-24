package com.example.despacho.service;

import com.example.despacho.common.AppProperties;
import com.example.despacho.dto.EventoDespacho;
import com.example.despacho.model.EstadoDespacho;
import com.example.despacho.repository.PaqueteRepository;
import com.example.despacho.repository.DespachoRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * OE12: gestiona el ciclo de vida de una suscripción de larga duración con
 * Disposable + doFinally + @PreDestroy, en vez de dejarla correr para
 * siempre sin forma de detenerla ordenadamente.
 */
@Component
public class ExpiracionJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiracionJob.class);

    private final DespachoRepository despachos;
    private final PaqueteRepository paquetes;
    private final AsignacionSaga saga;
    private final EventBus bus;
    private final AppProperties props;
    private Disposable suscripcion;

    public ExpiracionJob(DespachoRepository despachos, PaqueteRepository paquetes, AsignacionSaga saga,
                         EventBus bus, AppProperties props) {
        this.despachos = despachos;
        this.paquetes = paquetes;
        this.saga = saga;
        this.bus = bus;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        suscripcion = Flux.interval(props.expiryInterval())
                .onBackpressureDrop(t -> log.warn("Tick {} descartado: el lote anterior aún corre", t))
                .concatMap(t -> expirarLote()
                        .doOnNext(n -> { if (n > 0) log.info("Expiradas {} asignaciones vencidas", n); })
                        .onErrorResume(e -> {
                            log.error("Error al expirar asignaciones", e);
                            return Mono.empty();
                        }))
                .retry()
                .subscribe();
        log.info("Job de expiración de asignaciones iniciado, interval: {}", props.expiryInterval());
    }

    private Mono<Long> expirarLote() {
        return despachos.findByEstadoAndExpiraEnBefore(EstadoDespacho.ASIGNADO, Instant.now())
                .concatMap(d -> paquetes.findByDespachoId(d.getId())
                        .collectList()
                        .flatMap(saga::liberarTodo)
                        .then(Mono.defer(() -> {
                            d.setEstado(EstadoDespacho.EXPIRADO);
                            d.setExpiraEn(null);
                            return despachos.save(d);
                        }))
                        .doOnNext(saved -> bus.publicar(EventoDespacho.de(saved.getId(), EstadoDespacho.EXPIRADO,
                                "Asignación expirada, cupo liberado", saved.getTrazaId())))
                        .onErrorResume(e -> {
                            log.error("Error al expirar despacho {}: {}", d.getId(), e.toString());
                            return Mono.empty();
                        }))
                .count();
    }

    @PreDestroy
    public void detener() {
        if (suscripcion != null && !suscripcion.isDisposed()) {
            suscripcion.dispose();
            log.info("Job de expiración de asignaciones detenido");
        }
    }
}
