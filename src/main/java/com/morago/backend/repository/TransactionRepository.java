package com.morago.backend.repository;

import com.morago.backend.entity.Transaction;
import com.morago.backend.entity.enumFiles.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    boolean existsByCorrelationId(String correlationId);
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, TransactionType type, Pageable pageable);
}
