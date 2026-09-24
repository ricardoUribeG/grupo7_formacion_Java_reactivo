package com.example.despacho.dto;

public record ReporteCiudad(String ciudad, long despachos, double totalKg, double totalValor) {

    public static ReporteCiudad vacio(String ciudad) {
        return new ReporteCiudad(ciudad, 0, 0.0, 0.0);
    }

    public ReporteCiudad mas(double kg, double valor) {
        return new ReporteCiudad(ciudad, despachos + 1,
                Math.round((totalKg + kg) * 100.0) / 100.0,
                Math.round((totalValor + valor) * 100.0) / 100.0);
    }
}
