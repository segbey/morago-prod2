package com.morago.backend.exception.language;

public class LanguageInUseException extends RuntimeException {
    public LanguageInUseException(Long id) {
        super("Language " + id + " is in use by one or more translators");
    }
}