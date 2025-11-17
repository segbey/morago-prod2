package com.morago.backend.repository;

import com.morago.backend.entity.TranslatorProfile;
import com.morago.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TranslatorProfileRepository extends JpaRepository<TranslatorProfile, Long> {

    Optional<TranslatorProfile> findByUserId(Long userId);
    Optional<TranslatorProfile> findByUser(User user);
    Optional<TranslatorProfile> findByEmail(String email);

    boolean existsByUser_Id(Long userId);


    List<TranslatorProfile> findByIsVerifiedTrue();

    List<TranslatorProfile> findByIsOnlineTrue();

    List<TranslatorProfile> findByIsVerifiedTrueAndIsOnlineTrue();

    List<TranslatorProfile> findByEmailContainingIgnoreCase(String email);

    List<TranslatorProfile> findByLevelOfKoreanContainingIgnoreCase(String levelOfKorean);

    List<TranslatorProfile> findByLanguages_Id(Long languageId);

    List<TranslatorProfile> findByThemes_Id(Long themeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TranslatorProfile t " +
            "set t.ratingAvg = :avg, t.ratingCount = :count " +
            "where t.id = :id")
    void updateRatingStats(@Param("id") Long id,
                          @Param("avg") BigDecimal avg,
                          @Param("count") Integer count);

    @EntityGraph(
            attributePaths = { "languages", "themes", "user" },
            type = EntityGraph.EntityGraphType.LOAD
    )
    @Query(value = """
    SELECT tp
    FROM TranslatorProfile tp
    WHERE (:themeId IS NULL OR EXISTS (
            SELECT 1 FROM tp.themes t WHERE t.id = :themeId
          ))
      AND (:online   IS NULL OR tp.isOnline   = :online)
      AND (:verified IS NULL OR tp.isVerified = :verified)
      AND :requiredCount = (
            SELECT COUNT(DISTINCT l.id)
            FROM tp.languages l
            WHERE l.id IN :languageIds
          )
    """,
            countQuery = """
    SELECT COUNT(tp)
    FROM TranslatorProfile tp
    WHERE (:themeId IS NULL OR EXISTS (
            SELECT 1 FROM tp.themes t WHERE t.id = :themeId
          ))
      AND (:online   IS NULL OR tp.isOnline   = :online)
      AND (:verified IS NULL OR tp.isVerified = :verified)
      AND :requiredCount = (
            SELECT COUNT(DISTINCT l.id)
            FROM tp.languages l
            WHERE l.id IN :languageIds
          )
    """)
    Page<TranslatorProfile> searchByLanguagesThemeAndFlags(
            Collection<Long> languageIds,
            Long themeId,
            Boolean online,
            Boolean verified,
            long requiredCount,
            Pageable pageable
    );
}
