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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_use_cancel")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointUseCancel extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long useCancelId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private PointTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_use_id", nullable = false)
    private PointUse targetUse;

    @Column(name = "cancel_amount", nullable = false)
    private long cancelAmount;

    public PointUseCancel(PointTransaction transaction, PointUse targetUse, long cancelAmount) {
        this.transaction = transaction;
        this.targetUse = targetUse;
        this.cancelAmount = cancelAmount;
    }
}
