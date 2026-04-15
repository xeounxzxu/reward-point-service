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
@Table(name = "point_earn_cancel")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointGrantCancel extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long grantCancelId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private PointTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_earn_lot_id", nullable = false)
    private PointGrant targetGrant;

    @Column(name = "cancel_amount", nullable = false)
    private long cancelAmount;

    public PointGrantCancel(PointTransaction transaction, PointGrant targetGrant, long cancelAmount) {
        this.transaction = transaction;
        this.targetGrant = targetGrant;
        this.cancelAmount = cancelAmount;
    }
}
