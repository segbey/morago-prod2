package com.morago.backend.repository;

import com.morago.backend.entity.Language;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LanguageRepository extends JpaRepository<Language, Long> {
    Page<Language> findByNameContainingIgnoreCase(String q, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    List<Language> findAllByOrderByNameAsc();

    boolean existsByTranslatorProfiles_Id(Long translatorProfileId);
}
