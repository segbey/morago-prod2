package com.morago.backend.repository;

import com.morago.backend.entity.Theme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Page<Theme> findByNameContainingIgnoreCase(String q, Pageable pageable);
}