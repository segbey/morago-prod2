package com.morago.backend.repository;

import com.morago.backend.entity.Withdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    Page<Withdrawal> findByStatus(String status, Pageable pageable);

    Page<Withdrawal> findByUser_Id(Long userId, Pageable pageable);
    Page<Withdrawal> findByUser_IdAndStatus(Long userId, String status, Pageable pageable);
    boolean existsByUser_IdAndStatus(Long userId, String status);
}
