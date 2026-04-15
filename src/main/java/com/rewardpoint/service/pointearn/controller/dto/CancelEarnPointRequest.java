package com.rewardpoint.service.pointearn.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CancelEarnPointRequest(
        @NotNull Long accountId,
        @NotBlank String targetTransactionKey,
        String description
) {
}
