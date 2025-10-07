package com.morago.backend.repository;

import com.morago.backend.entity.Call;
import com.morago.backend.entity.enumFiles.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallRepository extends JpaRepository<Call, Long> {
    List<Call> findByCaller_IdOrRecipient_Id(Long callerId, Long recipientId);

    boolean existsByCaller_IdAndRecipient_IdAndTranslatorHasJoinedTrueAndEndCallTrueAndCallStatus(
            Long callerId, Long recipientId, CallStatus callStatus
    );

    Optional<Call> findTopByCaller_IdAndRecipient_IdAndTranslatorHasJoinedTrueAndEndCallTrueOrderByCreatedAtDesc(
            Long callerId, Long recipientId);

    Page<Call> findByCaller_Id(Long userId, Pageable pageable);
    Page<Call> findByRecipient_Id(Long userId, Pageable pageable);
    Page<Call> findByCaller_IdOrRecipient_Id(Long callerId, Long recipientId, Pageable pageable);

    boolean existsByCaller_IdAndEndCallFalse(Long userId);
    boolean existsByRecipient_IdAndEndCallFalse(Long userId);
}