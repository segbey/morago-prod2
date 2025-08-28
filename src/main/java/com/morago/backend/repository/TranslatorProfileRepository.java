package com.morago.backend.repository;

import com.morago.backend.entity.TranslatorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslatorProfileRepository extends JpaRepository<TranslatorProfile, Long> {

    Optional<TranslatorProfile> findByUserId(Long userId);
    boolean existsByUser_Id(Long userId);


    List<TranslatorProfile> findByIsVerifiedTrue();

    List<TranslatorProfile> findByIsOnlineTrue();

    List<TranslatorProfile> findByIsVerifiedTrueAndIsOnlineTrue();

    List<TranslatorProfile> findByEmailContainingIgnoreCase(String email);

    List<TranslatorProfile> findByLevelOfKoreanContainingIgnoreCase(String levelOfKorean);

    List<TranslatorProfile> findByLanguages_Id(Long languageId);

    List<TranslatorProfile> findByThemes_Id(Long themeId);
}
