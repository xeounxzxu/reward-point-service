package com.rewardpoint.service.pointledger.entity;

import com.rewardpoint.service.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_expiration_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointExpirationHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_expiration_history_id")
    private Long expirationHistoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "earn_lot_id", nullable = false)
    private PointGrant grant;

    @Column(name = "expired_amount", nullable = false)
    private long expiredAmount;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    public PointExpirationHistory(PointGrant grant, long expiredAmount, LocalDateTime expiredAt) {
        this.grant = grant;
        this.expiredAmount = expiredAmount;
        this.expiredAt = expiredAt;
    }
}
