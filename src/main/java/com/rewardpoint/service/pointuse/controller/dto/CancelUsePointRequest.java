package com.rewardpoint.service.pointuse.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CancelUsePointRequest(
        @NotNull Long accountId,
        @NotBlank String targetTransactionKey,
        @Min(1) long cancelAmount,
        String description
) {
}
