package com.morago.backend.service.category;

import com.morago.backend.dto.CategoryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    Page<CategoryDto> listCategories(Pageable pageable, String q);
    Object createCategory(String name);
    Object updateCategory(Long id, String name);
    void deleteCategory(Long id);
}
