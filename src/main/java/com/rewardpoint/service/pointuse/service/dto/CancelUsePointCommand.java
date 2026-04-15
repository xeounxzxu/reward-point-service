package com.rewardpoint.service.pointuse.service.dto;

public record CancelUsePointCommand(
        Long accountId,
        String targetTransactionKey,
        long cancelAmount,
        String description
) {
}
