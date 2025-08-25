package com.morago.backend.exception;

import org.springframework.http.HttpStatus;

public class PasswordRequiredException extends ApiException {
    public PasswordRequiredException() {
        super(HttpStatus.BAD_REQUEST, "PASSWORD_REQUIRED", "Password is required");
    }
}