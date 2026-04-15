package com.rewardpoint.service.pointledger.support.dto;

import com.rewardpoint.service.pointledger.entity.PointTransactionType;
import java.util.List;

public record PointOperationResult(
        Long accountId,
        String transactionKey,
        PointTransactionType transactionType,
        long amount,
        long currentBalance,
        String orderNo,
        List<PointOperationLineResult> lines
) {
}
