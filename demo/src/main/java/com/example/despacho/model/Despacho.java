package com.example.despacho.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Table("despacho")
public class Despacho {

    @Id
    private Long id;
    private Long clienteId;
    private String ciudad;
    private String idempotencyKey;
    private EstadoDespacho estado;
    private Double tarifa;
    private Integer minutosEntrega;
    private Integer scoreRiesgo;
    private Double totalKg;
    private Instant creadoEn;
    private Instant expiraEn;
    private String trazaId;

    @Transient
    private List<Paquete> paquetes = new ArrayList<>();

    public Despacho() {}

    public static Despacho recibido(Long clienteId, String ciudad, String idempotencyKey, String trazaId) {
        Despacho d = new Despacho();
        d.clienteId = clienteId;
        d.ciudad = ciudad;
        d.idempotencyKey = idempotencyKey;
        d.estado = EstadoDespacho.RECIBIDO;
        d.creadoEn = Instant.now();
        d.trazaId = trazaId;
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public EstadoDespacho getEstado() { return estado; }
    public void setEstado(EstadoDespacho estado) { this.estado = estado; }
    public Double getTarifa() { return tarifa; }
    public void setTarifa(Double tarifa) { this.tarifa = tarifa; }
    public Integer getMinutosEntrega() { return minutosEntrega; }
    public void setMinutosEntrega(Integer minutosEntrega) { this.minutosEntrega = minutosEntrega; }
    public Integer getScoreRiesgo() { return scoreRiesgo; }
    public void setScoreRiesgo(Integer scoreRiesgo) { this.scoreRiesgo = scoreRiesgo; }
    public Double getTotalKg() { return totalKg; }
    public void setTotalKg(Double totalKg) { this.totalKg = totalKg; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
    public Instant getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }
    public String getTrazaId() { return trazaId; }
    public void setTrazaId(String trazaId) { this.trazaId = trazaId; }
    public List<Paquete> getPaquetes() { return paquetes; }
    public void setPaquetes(List<Paquete> paquetes) { this.paquetes = paquetes == null ? new ArrayList<>() : paquetes; }
}