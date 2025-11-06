package com.morago.backend.exception.category;

import com.morago.backend.exception.common.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException(Long id) {
        super("Category", id);
    }
}