package com.rewardpoint.service.pointearn.service.dto;

public record EarnPointCommand(
        Long accountId,
        long amount,
        boolean manual,
        Integer expireDays,
        String description
) {
}
