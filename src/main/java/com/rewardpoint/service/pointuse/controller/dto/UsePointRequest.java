package com.rewardpoint.service.pointuse.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsePointRequest(
        @NotNull Long accountId,
        @NotBlank String orderNo,
        @Min(1) long amount,
        String description
) {
}
