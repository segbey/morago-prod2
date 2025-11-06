package com.morago.backend.service.category;

import com.morago.backend.dto.CategoryDto;
import com.morago.backend.entity.Category;
import com.morago.backend.exception.category.CategoryNotFoundException;
import com.morago.backend.mapper.CategoryMapper;
import com.morago.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDto> listCategories(Pageable pageable, String q) {
        if (q != null && !q.isBlank()) {
            return categoryRepository.findByNameContainingIgnoreCase(q.trim(), pageable)
                    .map(categoryMapper::toDto);
        }
        return categoryRepository.findAll(pageable).map(categoryMapper::toDto);
    }

    @Override
    @Transactional
    public Object createCategory(String name) {
        var category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Object updateCategory(Long id, String name) {
        var category = findByIdOrThrow(id);
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        var category = findByIdOrThrow(id);
        categoryRepository.delete(category);
    }

    public Category findByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }
}