package com.morago.backend.exception.theme;

import com.morago.backend.exception.common.NotFoundException;

public class ThemeNotFoundException extends NotFoundException {
    public ThemeNotFoundException(Long id) { super("Theme", id); }
}