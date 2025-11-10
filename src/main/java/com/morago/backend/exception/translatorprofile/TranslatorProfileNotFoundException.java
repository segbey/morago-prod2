package com.morago.backend.exception.translatorprofile;

import com.morago.backend.exception.common.NotFoundException;

public class TranslatorProfileNotFoundException extends NotFoundException {
    public TranslatorProfileNotFoundException(Long id) {
        super("TranslatorProfile", id);
    }
    public TranslatorProfileNotFoundException(String by) {
        super("TranslatorProfile", by);
    }
}