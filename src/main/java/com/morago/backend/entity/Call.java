package com.morago.backend.entity;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "calls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Call {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "caller_id", length = 50)
    private String callerId;

    @Column(name = "recipient_id", length = 50)
    private String recipientId;

    @Column(name = "theme_id")
    private Long themeId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "sum_decimal", precision = 10, scale = 2)
    private BigDecimal sumDecimal;

    @Column(name = "commission", precision = 10, scale = 2)
    private BigDecimal commission;

    @Column(name = "translator_has_joined")
    private Boolean translatorHasJoined;

    @Column(name = "user_has_rated")
    private Boolean userHasRated;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "channel_name", length = 50)
    private String channelName;

    @Column(name = "call_status")
    private Integer callStatus;

    @Column(name = "is_end_call")
    private Boolean isEndCall;


}

