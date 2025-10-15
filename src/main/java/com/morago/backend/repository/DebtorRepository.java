package com.morago.backend.repository;

import com.morago.backend.entity.Debtor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DebtorRepository extends JpaRepository<Debtor, Long> {
    Optional<Debtor> findByUserIdAndIsPaidFalse(Long userId);

    List<Debtor> findByUserIdAndIsPaidFalseOrderByCreatedAtAsc(Long userId);

    Optional<Debtor> findFirstByUserIdAndIsPaidFalseOrderByCreatedAtAsc(Long userId);

    boolean existsByUserIdAndIsPaidFalse(Long userId);
}
