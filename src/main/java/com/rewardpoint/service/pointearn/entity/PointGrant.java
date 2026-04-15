package com.rewardpoint.service.pointearn.entity;

import com.rewardpoint.service.global.entity.BaseTimeEntity;
import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointcore.entity.PointTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_earn_lot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointGrant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "earn_lot_id")
    private Long grantId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private PointTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private PointAccount account;

    @Enumerated(EnumType.STRING)
    @Column(name = "earn_type", nullable = false, length = 30)
    private PointGrantType grantType;

    @Column(name = "original_amount", nullable = false)
    private long originalAmount;

    @Column(name = "remaining_amount", nullable = false)
    private long remainingAmount;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(nullable = false)
    private boolean manual;

    public PointGrant(
            PointTransaction transaction,
            PointAccount account,
            PointGrantType grantType,
            long amount,
            LocalDateTime expireAt,
            boolean manual
    ) {
        this.transaction = transaction;
        this.account = account;
        this.grantType = grantType;
        this.originalAmount = amount;
        this.remainingAmount = amount;
        this.expireAt = expireAt;
        this.manual = manual;
    }

    public void use(long amount) {
        if (remainingAmount < amount) {
            throw new IllegalArgumentException("지급 건 잔액이 부족합니다.");
        }
        remainingAmount -= amount;
    }

    public void restore(long amount) {
        remainingAmount += amount;
    }

    public void expireAll() {
        remainingAmount = 0L;
    }

    public void cancelAll() {
        remainingAmount = 0L;
    }

    public boolean isFullyUnused() {
        return originalAmount == remainingAmount;
    }

    public boolean isExpired(LocalDateTime now) {
        return expireAt.isBefore(now) || expireAt.isEqual(now);
    }

    public long getCancelableAmount() {
        return remainingAmount;
    }
}
