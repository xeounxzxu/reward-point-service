package com.rewardpoint.service.pointcore.support.dto;

import com.rewardpoint.service.pointcore.entity.PointTransactionType;
import com.rewardpoint.service.pointcore.support.dto.PointOperationResult;
import java.util.List;

public record PointOperationResponse(
        Long accountId,
        String transactionKey,
        PointTransactionType transactionType,
        long amount,
        long currentBalance,
        String orderNo,
        List<PointOperationLineResponse> lines
) {
    public static PointOperationResponse from(PointOperationResult result) {
        return new PointOperationResponse(
                result.accountId(),
                result.transactionKey(),
                result.transactionType(),
                result.amount(),
                result.currentBalance(),
                result.orderNo(),
                result.lines().stream().map(PointOperationLineResponse::from).toList()
        );
    }
}
