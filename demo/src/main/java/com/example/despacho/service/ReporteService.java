package com.example.despacho.service;

import com.example.despacho.dto.ReporteCiudad;
import com.example.despacho.model.Despacho;
import com.example.despacho.repository.DespachoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReporteService {

    /** Tamaño de lote que el consumidor pide aguas arriba a la vez (OE6: backpressure real). */
    private static final int LOTE = 50;

    private final DespachoRepository despachos;
    private final EventBus bus;

    public ReporteService(DespachoRepository despachos, EventBus bus) {
        this.despachos = despachos;
        this.bus = bus;
    }

    /** GET /api/reports/ciudades: totales finales, respetando backpressure con limitRate. */
    public Flux<ReporteCiudad> totalesPorCiudad() {
        return despachos.findAll()
                .limitRate(LOTE)
                .filter(d -> d.getTotalKg() != null)
                .groupBy(Despacho::getCiudad)
                .flatMap(grupo -> grupo.reduce(ReporteCiudad.vacio(grupo.key()),
                        (acc, d) -> acc.mas(d.getTotalKg(), d.getTarifa() == null ? 0 : d.getTarifa())));
    }

    /**
     * GET /api/reports/ciudades/stream: snapshot histórico + lo que se va
     * asignando en vivo, acumulado con scan (cada elemento emitido es el
     * "hasta ahora", no solo el delta) -> application/x-ndjson.
     */
    public Flux<ReporteCiudad> acumuladoEnVivo() {
        Flux<Despacho> historicos = despachos.findAll().limitRate(LOTE).filter(d -> d.getTotalKg() != null);
        Flux<Despacho> vivos = bus.despachosAsignados();

        return Flux.concat(historicos, vivos)
                .scan(new LinkedHashMap<String, ReporteCiudad>(), (mapa, d) -> {
                    Map<String, ReporteCiudad> copia = new LinkedHashMap<>(mapa);
                    ReporteCiudad actual = copia.getOrDefault(d.getCiudad(), ReporteCiudad.vacio(d.getCiudad()));
                    copia.put(d.getCiudad(), actual.mas(d.getTotalKg(), d.getTarifa() == null ? 0 : d.getTarifa()));
                    return copia;
                })
                .skip(1)
                .concatMap(mapa -> Flux.fromIterable(mapa.values()));
    }
}
