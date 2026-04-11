package com.rewardpoint.service.pointaccount.entity;

import com.rewardpoint.service.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "point_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_point_account_user_id", columnNames = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointAccountStatus status;

    @Column(name = "current_balance", nullable = false)
    private long currentBalance;

    @Version
    @Column(nullable = false)
    private Long version;

    public PointAccount(String userId) {
        this.userId = userId;
        this.status = PointAccountStatus.ACTIVE;
        this.currentBalance = 0L;
    }

    public void charge(long amount) {
        this.currentBalance += amount;
    }

    public void use(long amount) {
        if (this.currentBalance < amount) {
            throw new IllegalArgumentException("Not enough balance.");
        }
        this.currentBalance -= amount;
    }

    public void lock() {
        this.status = PointAccountStatus.LOCKED;
    }

    public void activate() {
        this.status = PointAccountStatus.ACTIVE;
    }
}
