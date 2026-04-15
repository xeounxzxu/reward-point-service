package com.rewardpoint.service.pointledger.entity;

import com.rewardpoint.service.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_policy_id")
    private Long policyId;

    @Column(nullable = false)
    private long maxEarnAmountPerRequest;

    @Column(nullable = false)
    private long maxFreePointBalance;

    @Column(nullable = false)
    private int defaultExpireDays;

    @Column(nullable = false)
    private int minExpireDays;

    @Column(nullable = false)
    private int maxExpireDays;

    @Column(nullable = false)
    private boolean active;

    public PointPolicy(
            long maxEarnAmountPerRequest,
            long maxFreePointBalance,
            int defaultExpireDays,
            int minExpireDays,
            int maxExpireDays,
            boolean active
    ) {
        this.maxEarnAmountPerRequest = maxEarnAmountPerRequest;
        this.maxFreePointBalance = maxFreePointBalance;
        this.defaultExpireDays = defaultExpireDays;
        this.minExpireDays = minExpireDays;
        this.maxExpireDays = maxExpireDays;
        this.active = active;
    }

    public void validateEarnAmount(long amount) {
        if (amount < 1 || amount > maxEarnAmountPerRequest) {
            throw new IllegalArgumentException(
                    "적립 금액은 1 이상 " + maxEarnAmountPerRequest + " 이하여야 합니다."
            );
        }
    }

    public void validateMaxBalance(long currentBalance, long amount) {
        if (currentBalance + amount > maxFreePointBalance) {
            throw new IllegalArgumentException(
                    "무료 포인트 보유 한도는 " + maxFreePointBalance + " 을 초과할 수 없습니다."
            );
        }
    }

    public int resolveExpireDays(Integer expireDays) {
        int resolved = expireDays == null ? defaultExpireDays : expireDays;
        if (resolved < minExpireDays || resolved >= maxExpireDays) {
            throw new IllegalArgumentException(
                    "만료일은 " + minExpireDays + "일 이상 " + maxExpireDays + "일 미만이어야 합니다."
            );
        }
        return resolved;
    }
}
