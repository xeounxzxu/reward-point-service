package com.rewardpoint.service.pointledger.entity;

import com.rewardpoint.service.global.entity.BaseTimeEntity;
import com.rewardpoint.service.pointaccount.entity.PointAccount;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "point_transaction",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_point_transaction_point_key", columnNames = "point_key")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(name = "point_key", nullable = false, length = 36)
    private String transactionKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private PointAccount account;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private PointTransactionType transactionType;

    @Column(nullable = false)
    private long amount;

    @Column(name = "origin_transaction_id")
    private Long originTransactionId;

    @Column(name = "order_no", length = 50)
    private String orderNo;

    @Column(length = 255)
    private String description;

    public PointTransaction(
            String transactionKey,
            PointAccount account,
            PointTransactionType transactionType,
            long amount,
            Long originTransactionId,
            String orderNo,
            String description
    ) {
        this.transactionKey = transactionKey;
        this.account = account;
        this.transactionType = transactionType;
        this.amount = amount;
        this.originTransactionId = originTransactionId;
        this.orderNo = orderNo;
        this.description = description;
    }
}
