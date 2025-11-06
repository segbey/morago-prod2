package com.morago.backend.exception.language;

import com.morago.backend.exception.common.NotFoundException;

public class LanguageNotFoundException extends NotFoundException {
    public LanguageNotFoundException(Long id) {
        super("Language", id);
    }
}