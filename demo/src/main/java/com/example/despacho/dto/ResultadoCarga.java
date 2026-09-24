package com.example.despacho.dto;

public record ResultadoCarga(int guardados, int fallidos) {
    public ResultadoCarga mas(ResultadoCarga otro) {
        return new ResultadoCarga(guardados + otro.guardados, fallidos + otro.fallidos);
    }
    public static ResultadoCarga vacio() { return new ResultadoCarga(0, 0); }
}
