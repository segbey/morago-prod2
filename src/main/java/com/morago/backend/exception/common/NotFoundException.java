package com.morago.backend.exception.common;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {
    private final String entity;
    private final Object id;

    public NotFoundException(String entity, Object id) {
        super(entity + " not found: " + id);
        this.entity = entity;
        this.id = id;
    }
}