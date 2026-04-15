package com.rewardpoint.service.pointcore.support.dto;

import com.rewardpoint.service.pointcore.entity.PointTransactionType;
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
