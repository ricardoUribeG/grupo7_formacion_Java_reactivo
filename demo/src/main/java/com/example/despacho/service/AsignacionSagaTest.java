package com.example.despacho.service;

import com.example.despacho.common.DomainExceptions.CupoInsuficienteException;
import com.example.despacho.dto.CrearDespachoRequest.PaqueteItem;
import com.example.despacho.model.Vehiculo;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AsignacionSagaTest {

    private final CupoService cupoService = mock(CupoService.class);
    private final AsignacionSaga saga = new AsignacionSaga(cupoService);

    @Test
    void reservarTodos_siTodoOk_devuelveLosPaquetesReservados() {
        when(cupoService.reservar(1L, 50.0)).thenReturn(Mono.just(vehiculo(1L, 450)));
        when(cupoService.reservar(2L, 30.0)).thenReturn(Mono.just(vehiculo(2L, 270)));

        List<PaqueteItem> pedidos = List.of(new PaqueteItem(1L, 50.0), new PaqueteItem(2L, 30.0));

        StepVerifier.create(saga.reservarTodos(pedidos, 100L))
                .assertNext(paquetes -> {
                    org.assertj.core.api.Assertions.assertThat(paquetes).hasSize(2);
                    org.assertj.core.api.Assertions.assertThat(paquetes.get(0).getVehiculoId()).isEqualTo(1L);
                })
                .verifyComplete();

        verify(cupoService, never()).liberar(anyLong(), anyDouble());
    }

    @Test
    void reservarTodos_siFallaAMitad_compensaSoloLoYaReservadoYPropagaElError() {
        when(cupoService.reservar(1L, 50.0)).thenReturn(Mono.just(vehiculo(1L, 450)));
        when(cupoService.reservar(2L, 999.0))
                .thenReturn(Mono.error(new CupoInsuficienteException(2L, 999.0)));
        when(cupoService.liberar(eq(1L), anyDouble())).thenReturn(Mono.just(vehiculo(1L, 500)));

        List<PaqueteItem> pedidos = List.of(new PaqueteItem(1L, 50.0), new PaqueteItem(2L, 999.0));

        StepVerifier.create(saga.reservarTodos(pedidos, 100L))
                .expectError(CupoInsuficienteException.class)
                .verify();

        verify(cupoService, times(1)).liberar(eq(1L), eq(50.0));
        verify(cupoService, never()).liberar(eq(2L), anyDouble());
    }

    @Test
    void liberarTodo_conListaVacia_completaSinLlamarNada() {
        StepVerifier.create(saga.liberarTodo(List.of())).verifyComplete();
        verifyNoInteractions(cupoService);
    }

    private Vehiculo vehiculo(Long id, int cupo) {
        return new Vehiculo(id, "PLACA" + id, "BOG", cupo, 500);
    }
}
