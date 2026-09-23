package com.example.despacho.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CrearDespachoRequest(
        @NotNull(message = "clienteId es obligatorio") Long clienteId,
        @NotBlank(message = "ciudad es obligatoria") String ciudad,
        @NotEmpty(message = "el despacho debe tener al menos un paquete") @Valid List<PaqueteItem> paquetes) {

    public record PaqueteItem(
            @NotNull(message = "vehiculoId es obligatorio") Long vehiculoId,
            @NotNull(message = "pesoKg es obligatorio") @Positive(message = "pesoKg debe ser > 0") Double pesoKg) {}
}