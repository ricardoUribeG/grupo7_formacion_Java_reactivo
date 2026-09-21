package com.example.demo.service;

import com.example.demo.model.ItemOrden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class ReservaSaga {
    private final InventarioPort inventario;
    private static final Logger log = LoggerFactory.getLogger(ReservaSaga.class);

    public ReservaSaga(InventarioPort inventario) {
        this.inventario = inventario;
    }

    public Mono<List<ItemOrden>> reservarTodos(List<ItemOrden> items, Long ordenId) {
        return Mono.defer(() -> {
            List<ItemOrden> hechos = new ArrayList<>();
            return Flux.fromIterable(items)
                    .concatMap(i -> inventario.reservar(i.getProductoId(), i.getCantidad(), ordenId)
                            .map(p -> new ItemOrden(ordenId, i.getProductoId(), p.getCategory(),i.getCantidad(), p.getPrice())))
                    .doOnNext(hechos::add)
                    .collectList()
                    .onErrorResume(ex -> {
                                log.warn("Reserva fallida en orden {}: {}. Compensando {} ítems", ordenId, ex.getMessage(), hechos.size());
                                return liberarTodo(hechos, ordenId).then(Mono.error(ex));
                    });
        });
    }

    public Mono<Void> liberarTodo(List<ItemOrden> items, Long ordenId) {
        if(items == null || items.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(items)
                .concatMap(i -> inventario.liberar(i.getProductoId(), i.getCantidad(), ordenId)
                        .onErrorResume(ex -> {
                            log.error("No se pudo liberar producto {} de orden {}: {}",  i.getProductoId(), ordenId, ex.toString());
                            return Mono.empty();
                        })
                )
                .then();
    }
}
