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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_use_allocation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointUseAllocation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long allocationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "use_id", nullable = false)
    private PointUse pointUse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "earn_lot_id", nullable = false)
    private PointGrant grant;

    @Column(name = "allocated_amount", nullable = false)
    private long allocatedAmount;

    @Column(name = "cancelled_amount", nullable = false)
    private long cancelledAmount;

    public PointUseAllocation(PointUse pointUse, PointGrant grant, long allocatedAmount) {
        this.pointUse = pointUse;
        this.grant = grant;
        this.allocatedAmount = allocatedAmount;
        this.cancelledAmount = 0L;
    }

    public long getRemainingCancelableAmount() {
        return allocatedAmount - cancelledAmount;
    }

    public void cancel(long amount) {
        if (getRemainingCancelableAmount() < amount) {
            throw new IllegalArgumentException("사용취소 금액이 allocation 금액을 초과할 수 없습니다.");
        }
        cancelledAmount += amount;
    }
}
