package com.morago.backend.exception.language;

public class LanguageAlreadyExistsException extends RuntimeException {
    public LanguageAlreadyExistsException(String name) {
        super("Language already exists: " + name);
    }
}
