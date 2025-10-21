package com.morago.backend.service.category;

import com.morago.backend.dto.CategoryDto;
import com.morago.backend.entity.Category;
import com.morago.backend.exception.ResourceNotFoundException;
import com.morago.backend.mapper.CategoryMapper;
import com.morago.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        var category = new com.morago.backend.entity.Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Object updateCategory(Long id, String name) {
        var category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found: " + id));
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) throw new RuntimeException("Category not found: " + id);
        categoryRepository.deleteById(id);
    }
}
