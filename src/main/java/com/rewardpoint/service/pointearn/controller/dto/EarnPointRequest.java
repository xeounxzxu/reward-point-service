package com.rewardpoint.service.pointearn.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EarnPointRequest(
        @NotNull Long accountId,
        @Min(1) @Max(100000) long amount,
        boolean manual,
        Integer expireDays,
        String description
) {
}
