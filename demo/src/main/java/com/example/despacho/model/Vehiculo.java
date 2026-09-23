package com.example.despacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("vehiculo")
public class Vehiculo {

    @Id
    private Long id;
    private String placa;
    private String ciudad;
    private Integer cupoKg;
    private Integer cupoKgOriginal;
    private Instant actualizadoEn;

    public Vehiculo() {}

    public Vehiculo(Long id, String placa, String ciudad, Integer cupoKg, Integer cupoKgOriginal) {
        this.id = id;
        this.placa = placa;
        this.ciudad = ciudad;
        this.cupoKg = cupoKg;
        this.cupoKgOriginal = cupoKgOriginal;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public Integer getCupoKg() { return cupoKg; }
    public void setCupoKg(Integer cupoKg) { this.cupoKg = cupoKg; }
    public Integer getCupoKgOriginal() { return cupoKgOriginal; }
    public void setCupoKgOriginal(Integer cupoKgOriginal) { this.cupoKgOriginal = cupoKgOriginal; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}