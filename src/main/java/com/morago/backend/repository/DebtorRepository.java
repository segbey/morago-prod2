package com.morago.backend.repository;

import com.morago.backend.entity.Debtor;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DebtorRepository extends JpaRepository<Debtor, Long> {
    Optional<Debtor> findByUserIdAndIsPaidFalse(Long userId);

    List<Debtor> findByUserIdAndIsPaidFalseOrderByCreatedAtAsc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    Optional<Debtor> findFirstByUserIdAndIsPaidFalseOrderByCreatedAtAsc(Long userId);

    boolean existsByUserIdAndIsPaidFalse(Long userId);
}
