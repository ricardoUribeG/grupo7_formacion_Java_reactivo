package com.example.despacho.common;

import org.springframework.http.HttpStatus;

/**
 * Errores esperados según el enunciado (sección "Errores esperados"):
 * VehiculoNoExisteException 404, CupoInsuficienteException 409,
 * ZonaRiesgosaException 422, DespachoNoExisteException 404,
 * EstadoInvalidoException 409, ValidacionException 400.
 */
public final class DomainExceptions {

    private DomainExceptions() {}

    public static class ValidacionException extends DomainException {
        public ValidacionException(String mensaje) {
            super(HttpStatus.BAD_REQUEST, "VALIDACION", mensaje);
        }
    }

    public static class VehiculoNoExisteException extends DomainException {
        public VehiculoNoExisteException(Long id) {
            super(HttpStatus.NOT_FOUND, "VEHICULO_NO_EXISTE", "Vehículo no existe: " + id);
        }
    }

    public static class CupoInsuficienteException extends DomainException {
        public CupoInsuficienteException(Long vehiculoId, double pesoKg) {
            super(HttpStatus.CONFLICT, "CUPO_INSUFICIENTE",
                    "Cupo insuficiente en vehículo " + vehiculoId + " para " + pesoKg + " kg");
        }
    }

    public static class ZonaRiesgosaException extends DomainException {
        public ZonaRiesgosaException(int score) {
            super(HttpStatus.UNPROCESSABLE_ENTITY, "ZONA_RIESGOSA", "Zona de riesgo alto: score " + score);
        }
    }

    public static class DespachoNoExisteException extends DomainException {
        public DespachoNoExisteException(Long id) {
            super(HttpStatus.NOT_FOUND, "DESPACHO_NO_EXISTE", "Despacho no existe: " + id);
        }
    }

    public static class EstadoInvalidoException extends DomainException {
        public EstadoInvalidoException(String mensaje) {
            super(HttpStatus.CONFLICT, "ESTADO_INVALIDO", mensaje);
        }
    }
}