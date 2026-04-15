package com.rewardpoint.service.pointearn.service.dto;

public record CancelEarnPointCommand(
        Long accountId,
        String targetTransactionKey,
        String description
) {
}
