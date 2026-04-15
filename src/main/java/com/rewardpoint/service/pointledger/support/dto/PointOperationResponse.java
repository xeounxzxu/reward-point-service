package com.rewardpoint.service.pointledger.support.dto;

import com.rewardpoint.service.pointledger.entity.PointTransactionType;
import com.rewardpoint.service.pointledger.support.dto.PointOperationResult;
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
