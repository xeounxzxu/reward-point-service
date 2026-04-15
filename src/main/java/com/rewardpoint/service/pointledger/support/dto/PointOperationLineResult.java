package com.rewardpoint.service.pointledger.support.dto;

public record PointOperationLineResult(
        String sourceTransactionKey,
        long amount,
        String restoreType,
        String reissuedTransactionKey
) {
}
