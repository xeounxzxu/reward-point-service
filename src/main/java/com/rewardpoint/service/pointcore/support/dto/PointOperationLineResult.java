package com.rewardpoint.service.pointcore.support.dto;

public record PointOperationLineResult(
        String sourceTransactionKey,
        long amount,
        String restoreType,
        String reissuedTransactionKey
) {
}
