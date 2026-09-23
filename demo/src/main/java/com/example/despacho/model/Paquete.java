package com.example.despacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("paquete")
public class Paquete {

    @Id
    private Long id;
    private Long despachoId;
    private Long vehiculoId;
    private Double pesoKg;

    public Paquete() {}

    public Paquete(Long despachoId, Long vehiculoId, Double pesoKg) {
        this.despachoId = despachoId;
        this.vehiculoId = vehiculoId;
        this.pesoKg = pesoKg;
    }

    public Paquete conDespacho(Long nuevoDespachoId) {
        return new Paquete(nuevoDespachoId, vehiculoId, pesoKg);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDespachoId() { return despachoId; }
    public void setDespachoId(Long despachoId) { this.despachoId = despachoId; }
    public Long getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }
    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }
}
