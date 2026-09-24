package com.example.despacho.service;

import com.example.despacho.dto.CrearDespachoRequest.PaqueteItem;
import com.example.despacho.model.Paquete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * T5: saga de compensación. Reserva paquete por paquete, EN ORDEN
 * (concatMap, no en paralelo: el orden de reserva importa para poder
 * compensar exactamente lo que ya se alcanzó a tomar). Si un paquete falla a
 * mitad de camino, libera únicamente lo ya reservado hasta ese punto y
 * propaga el error original.
 */
@Service
public class AsignacionSaga {

    private static final Logger log = LoggerFactory.getLogger(AsignacionSaga.class);

    private final CupoService cupoService;

    public AsignacionSaga(CupoService cupoService) {
        this.cupoService = cupoService;
    }

    public Mono<List<Paquete>> reservarTodos(List<PaqueteItem> pedidos, Long despachoId) {
        return Mono.defer(() -> {
            List<Paquete> hechos = new ArrayList<>();
            return Flux.fromIterable(pedidos)
                    .concatMap(p -> cupoService.reservar(p.vehiculoId(), p.pesoKg())
                            .map(v -> new Paquete(despachoId, p.vehiculoId(), p.pesoKg())))
                    .doOnNext(hechos::add)
                    .collectList()
                    .onErrorResume(ex -> {
                        log.warn("Reserva fallida en despacho {}: {}. Compensando {} paquetes",
                                despachoId, ex.getMessage(), hechos.size());
                        return liberarTodo(hechos).then(Mono.error(ex));
                    });
        });
    }

    public Mono<Void> liberarTodo(List<Paquete> paquetes) {
        if (paquetes == null || paquetes.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(paquetes)
                .concatMap(p -> cupoService.liberar(p.getVehiculoId(), p.getPesoKg())
                        .onErrorResume(ex -> {
                            log.error("No se pudo liberar vehículo {} del despacho {}: {}",
                                    p.getVehiculoId(), p.getDespachoId(), ex.toString());
                            return Mono.empty();
                        }))
                .then();
    }
}
