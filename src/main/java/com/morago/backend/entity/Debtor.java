package com.morago.backend.entity;

import com.morago.backend.listener.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "debtors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Debtor extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "account_holder", length = 200, nullable = true)
    private String accountHolder;

    @Column(name = "name_of_bank", length = 200, nullable = true)
    private String nameOfBank;

    @Column(name = "owed_decimal", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private java.math.BigDecimal owedDecimal = java.math.BigDecimal.ZERO;

    @Column(name = "is_paid")
    @Builder.Default
    private boolean isPaid = false;
}