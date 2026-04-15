package com.rewardpoint.service.pointledger.support.dto;

public record PointOperationLineResponse(
        String sourceTransactionKey,
        long amount,
        String restoreType,
        String reissuedTransactionKey
) {
    public static PointOperationLineResponse from(PointOperationLineResult result) {
        return new PointOperationLineResponse(
                result.sourceTransactionKey(),
                result.amount(),
                result.restoreType(),
                result.reissuedTransactionKey()
        );
    }
}
