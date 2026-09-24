package com.example.despacho.integration;

import com.example.despacho.common.DomainExceptions.CupoInsuficienteException;
import com.example.despacho.common.DomainExceptions.DespachoNoExisteException;
import com.example.despacho.common.DomainExceptions.ZonaRiesgosaException;
import com.example.despacho.common.GlobalErrorHandler;
import com.example.despacho.common.TrazaWebFilter;
import com.example.despacho.controller.DespachoController;
import com.example.despacho.dto.CrearDespachoRequest;
import com.example.despacho.model.Despacho;
import com.example.despacho.model.EstadoDespacho;
import com.example.despacho.service.DespachoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = DespachoController.class)
@Import({GlobalErrorHandler.class, TrazaWebFilter.class})
class DespachoControllerTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private DespachoService service;

    @Test
    void crear_conCuerpoValido_devuelve201ConElDespacho() {
        Despacho despacho = new Despacho();
        despacho.setId(1L);
        despacho.setClienteId(10L);
        despacho.setCiudad("BOG");
        despacho.setEstado(EstadoDespacho.ASIGNADO);

        when(service.crear(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.just(despacho));

        var body = new CrearDespachoRequest(10L, "BOG",
                List.of(new CrearDespachoRequest.PaqueteItem(1L, 50.0)));

        client.post().uri("/api/despachos")
                .header("X-Traza-Id", "traza-test-1")
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.estado").isEqualTo("ASIGNADO");
    }

    @Test
    void crear_conCuerpoSinPaquetes_devuelve400() {
        var body = new CrearDespachoRequest(10L, "BOG", List.of());

        client.post().uri("/api/despachos")
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void obtener_siNoExiste_devuelve404ConCuerpoUniforme() {
        when(service.obtener(999L)).thenReturn(Mono.error(new DespachoNoExisteException(999L)));

        client.get().uri("/api/despachos/999")
                .header("X-Traza-Id", "traza-test-2")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("DESPACHO_NO_EXISTE")
                .jsonPath("$.trazaId").isEqualTo("traza-test-2")
                .jsonPath("$.instante").exists();
    }

    @Test
    void crear_siCupoInsuficiente_devuelve409() {
        when(service.crear(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.error(new CupoInsuficienteException(1L, 999.0)));

        var body = new CrearDespachoRequest(10L, "BOG",
                List.of(new CrearDespachoRequest.PaqueteItem(1L, 999.0)));

        client.post().uri("/api/despachos")
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("CUPO_INSUFICIENTE");
    }

    @Test
    void crear_siZonaRiesgosa_devuelve422() {
        when(service.crear(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Mono.error(new ZonaRiesgosaException(95)));

        var body = new CrearDespachoRequest(10L, "BOG",
                List.of(new CrearDespachoRequest.PaqueteItem(1L, 50.0)));

        client.post().uri("/api/despachos")
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("ZONA_RIESGOSA");
    }
}
