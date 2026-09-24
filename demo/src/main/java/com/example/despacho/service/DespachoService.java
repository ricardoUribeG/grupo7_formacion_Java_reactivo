package com.example.despacho.service;

import com.example.despacho.common.AppProperties;
import com.example.despacho.common.DomainExceptions.DespachoNoExisteException;
import com.example.despacho.common.DomainExceptions.EstadoInvalidoException;
import com.example.despacho.common.DomainExceptions.ValidacionException;
import com.example.despacho.common.DomainExceptions.ZonaRiesgosaException;
import com.example.despacho.common.ReactiveSupport;
import com.example.despacho.common.TrazaWebFilter;
import com.example.despacho.dto.CrearDespachoRequest;
import com.example.despacho.dto.EventoDespacho;
import com.example.despacho.model.Despacho;
import com.example.despacho.model.EstadoDespacho;
import com.example.despacho.model.Paquete;
import com.example.despacho.repository.PaqueteRepository;
import com.example.despacho.repository.DespachoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DespachoService {

    private static final Logger log = LoggerFactory.getLogger(DespachoService.class);

    private final DespachoRepository despachos;
    private final PaqueteRepository paquetes;
    private final AsignacionSaga saga;
    private final TransportistaClient transportista;
    private final EventBus bus;
    private final AppProperties props;
    private final TransactionalOperator tx;

    public DespachoService(DespachoRepository despachos, PaqueteRepository paquetes, AsignacionSaga saga,
                           TransportistaClient transportista, EventBus bus, AppProperties props,
                           TransactionalOperator tx) {
        this.despachos = despachos;
        this.paquetes = paquetes;
        this.saga = saga;
        this.transportista = transportista;
        this.bus = bus;
        this.props = props;
        this.tx = tx;
    }

    // ---------- Crear (OE1..OE8) ----------

    public Mono<Despacho> crear(CrearDespachoRequest req, String idempotencyKey) {
        return validar(req)
                .then(buscarExistente(idempotencyKey))
                .switchIfEmpty(Mono.defer(() -> crearNuevo(req, idempotencyKey)));
    }

    private Mono<Void> validar(CrearDespachoRequest req) {
        if (req == null || req.paquetes() == null || req.paquetes().isEmpty()) {
            return Mono.error(new ValidacionException("El despacho debe tener al menos un paquete"));
        }
        if (req.clienteId() == null || req.ciudad() == null || req.ciudad().isBlank()) {
            return Mono.error(new ValidacionException("clienteId y ciudad son obligatorios"));
        }
        boolean pesoMalo = req.paquetes().stream()
                .anyMatch(p -> p.vehiculoId() == null || p.pesoKg() == null || p.pesoKg() <= 0);
        if (pesoMalo) {
            return Mono.error(new ValidacionException("Cada paquete requiere vehiculoId y pesoKg > 0"));
        }
        return Mono.empty();
    }

    private Mono<Despacho> buscarExistente(String key) {
        if (key == null || key.isBlank()) return Mono.empty();
        return despachos.findByIdempotencyKey(key)
                .doOnNext(d -> log.info("Idempotency-Key {} ya procesada -> despacho {}", key, d.getId()))
                .flatMap(this::conPaquetes);
    }

    private Mono<Despacho> crearNuevo(CrearDespachoRequest req, String key) {
        AtomicReference<List<Paquete>> reservados = new AtomicReference<>(List.of());

        Mono<Despacho> flujo = Mono.deferContextual(ctx -> {
            String trazaId = ctx.getOrDefault(TrazaWebFilter.CONTEXT_KEY, "sin-traza");
            return despachos.save(Despacho.recibido(req.clienteId(), req.ciudad(), key, trazaId));
        }).flatMap(despacho ->
                ReactiveSupport.traced("reserva-cupo", saga.reservarTodos(req.paquetes(), despacho.getId()))
                        .doOnNext(reservados::set)
                        .doOnNext(despacho::setPaquetes)
                        .flatMap(reservadosOk -> ReactiveSupport.traced("tarifa+clima+riesgo",
                                cotizar(despacho, reservadosOk)))
                        .flatMap(d -> d.getScoreRiesgo() > props.riskThreshold()
                                ? Mono.<Despacho>error(new ZonaRiesgosaException(d.getScoreRiesgo()))
                                : Mono.just(d))
                        .flatMap(this::persistirAsignado)
                        .doOnNext(d -> {
                            bus.publicar(EventoDespacho.de(d.getId(), d.getEstado(),
                                    "Cupo reservado, tarifa " + d.getTarifa(), d.getTrazaId()));
                            bus.publicarAsignado(d);
                        })
                        .onErrorResume(ex -> compensar(despacho, reservados.get(), ex)));

        return flujo
                .doOnSubscribe(s -> log.info("Creando despacho para cliente {} en {}", req.clienteId(), req.ciudad()))
                .doOnError(e -> log.warn("Despacho falló: {}", e.toString()));
    }

    /** OE2/OE3: tres llamadas externas independientes en paralelo con Mono.zip; cálculo en Schedulers.parallel(). */
    private Mono<Despacho> cotizar(Despacho despacho, List<Paquete> reservados) {
        double totalKg = reservados.stream().mapToDouble(Paquete::getPesoKg).sum();

        return Mono.zip(
                        transportista.tarifa(despacho.getCiudad()),
                        transportista.clima(despacho.getCiudad()),
                        transportista.riesgoZona(despacho.getCiudad()))
                .publishOn(Schedulers.parallel())
                .map(t -> {
                    double tarifaTotal = redondear(t.getT1().tarifaPorKg() * totalKg);
                    despacho.setTotalKg(redondear(totalKg));
                    despacho.setTarifa(tarifaTotal);
                    despacho.setMinutosEntrega(t.getT2().minutosEntrega());
                    despacho.setScoreRiesgo(t.getT3());
                    return despacho;
                });
    }

    /** OE8: despacho + paquetes en una sola transacción reactiva. */
    private Mono<Despacho> persistirAsignado(Despacho despacho) {
        despacho.setEstado(EstadoDespacho.ASIGNADO);
        despacho.setExpiraEn(Instant.now().plus(props.reservationTtl()));

        List<Paquete> aGuardar = despacho.getPaquetes().stream()
                .map(p -> p.conDespacho(despacho.getId()))
                .toList();

        Mono<Despacho> escritura = despachos.save(despacho)
                .flatMap(d -> paquetes.saveAll(aGuardar).collectList()
                        .doOnNext(d::setPaquetes)
                        .thenReturn(d));

        return escritura.as(tx::transactional);
    }

    /** Si ya se había reservado cupo, lo libera; persiste el estado final y re-emite el error original. */
    private Mono<Despacho> compensar(Despacho despacho, List<Paquete> reservados, Throwable ex) {
        EstadoDespacho estadoFinal = ex instanceof ZonaRiesgosaException
                ? EstadoDespacho.RECHAZADO
                : EstadoDespacho.COMPENSADO;
        return saga.liberarTodo(reservados)
                .then(Mono.defer(() -> {
                    despacho.setEstado(estadoFinal);
                    despacho.setExpiraEn(null);
                    return despachos.save(despacho);
                }))
                .doOnNext(d -> bus.publicar(EventoDespacho.de(d.getId(), estadoFinal, ex.getMessage(), d.getTrazaId())))
                .then(Mono.error(ex));
    }

    // ---------- Consultar / confirmar ----------

    public Mono<Despacho> obtener(Long id) {
        return despachos.findById(id)
                .switchIfEmpty(Mono.error(new DespachoNoExisteException(id)))
                .flatMap(this::conPaquetes);
    }

    private Mono<Despacho> conPaquetes(Despacho despacho) {
        return paquetes.findByDespachoId(despacho.getId())
                .collectList()
                .doOnNext(despacho::setPaquetes)
                .thenReturn(despacho);
    }

    /** Confirma: el cupo ya se descontó al reservar; aquí solo se consuma el estado. */
    public Mono<Despacho> confirmar(Long id) {
        return obtener(id)
                .flatMap(d -> d.getEstado() != EstadoDespacho.ASIGNADO
                        ? Mono.<Despacho>error(new EstadoInvalidoException("El despacho está en " + d.getEstado()))
                        : Mono.defer(() -> {
                    d.setEstado(EstadoDespacho.EN_RUTA);
                    d.setExpiraEn(null);
                    return despachos.save(d).doOnNext(saved -> saved.setPaquetes(d.getPaquetes()));
                }).as(tx::transactional))
                .doOnNext(d -> bus.publicar(EventoDespacho.de(d.getId(), d.getEstado(), "Despacho en ruta", d.getTrazaId())));
    }

    // ---------- Stream por despacho (OE9) ----------

    public Flux<EventoDespacho> eventos(Long id) {
        return obtener(id).flatMapMany(d -> {
            Flux<EventoDespacho> heartbeat = Flux.interval(Duration.ofSeconds(15))
                    .map(t -> EventoDespacho.heartbeat(id, d.getTrazaId()));
            Mono<EventoDespacho> actual = Mono.just(EventoDespacho.de(d.getId(), d.getEstado(), "estado actual", d.getTrazaId()));
            Flux<EventoDespacho> vivo = bus.eventosDespacho().filter(e -> id.equals(e.despachoId()));
            return Flux.merge(actual, vivo, heartbeat)
                    .takeUntil(e -> e.estado() != null && e.estado().esTerminado())
                    .doOnCancel(() -> log.info("Cliente cerró stream del despacho {}", id));
        });
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}