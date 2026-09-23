package com.example.despacho.service;

import com.example.despacho.dto.CotizacionPrecio;
import reactor.core.publisher.Mono;

public interface ServiciosExternosPort {
    Mono<CotizacionPrecio> precio(Long productoId);
    Mono<Double> tasaImpuesto(String region);
    Mono<Integer> scoreRiesgo(String clienteId, double totalEstimado);
}
