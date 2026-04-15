package com.rewardpoint.service.pointledger.entity;

import com.rewardpoint.service.global.entity.BaseTimeEntity;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_use_cancel_allocation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointUseCancelAllocation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long useCancelAllocationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "use_cancel_id", nullable = false)
    private PointUseCancel pointUseCancel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "use_allocation_id", nullable = false)
    private PointUseAllocation useAllocation;

    @Column(name = "restore_amount", nullable = false)
    private long restoreAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "restore_type", nullable = false, length = 30)
    private PointUseCancelRestoreType restoreType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reissued_earn_lot_id")
    private PointGrant reissuedGrant;

    public PointUseCancelAllocation(
            PointUseCancel pointUseCancel,
            PointUseAllocation useAllocation,
            long restoreAmount,
            PointUseCancelRestoreType restoreType,
            PointGrant reissuedGrant
    ) {
        this.pointUseCancel = pointUseCancel;
        this.useAllocation = useAllocation;
        this.restoreAmount = restoreAmount;
        this.restoreType = restoreType;
        this.reissuedGrant = reissuedGrant;
    }
}
