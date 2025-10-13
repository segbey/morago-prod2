package com.morago.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceAlreadyExistsException extends RuntimeException {

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }

    public ResourceAlreadyExistsException(String resourceName, Long id) {
        super(String.format("%s with id '%d' already exists", resourceName, id));
    }

    public ResourceAlreadyExistsException(String resourceName, String fieldValue) {
        super(String.format("%s with value '%s' already exists", resourceName, fieldValue));
    }
}
