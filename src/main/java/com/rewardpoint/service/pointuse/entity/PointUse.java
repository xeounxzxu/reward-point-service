package com.rewardpoint.service.pointuse.entity;

import com.rewardpoint.service.global.entity.BaseTimeEntity;
import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointcore.entity.PointTransaction;
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
@Table(name = "point_use")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointUse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "use_id")
    private Long useId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private PointTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private PointAccount account;

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;

    @Column(name = "used_amount", nullable = false)
    private long usedAmount;

    @Column(name = "cancelled_amount", nullable = false)
    private long cancelledAmount;

    public PointUse(PointTransaction transaction, PointAccount account, String orderNo, long usedAmount) {
        this.transaction = transaction;
        this.account = account;
        this.orderNo = orderNo;
        this.usedAmount = usedAmount;
        this.cancelledAmount = 0L;
    }

    public void cancel(long amount) {
        if (getRemainingCancelableAmount() < amount) {
            throw new IllegalArgumentException("사용취소 금액이 남은 사용 금액을 초과할 수 없습니다.");
        }
        cancelledAmount += amount;
    }

    public long getRemainingCancelableAmount() {
        return usedAmount - cancelledAmount;
    }
}
