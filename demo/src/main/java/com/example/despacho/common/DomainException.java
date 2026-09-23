package com.example.despacho.common;

import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    protected DomainException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus status() { return status; }
    public String codigo() { return codigo; }
}
