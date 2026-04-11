package com.rewardpoint.service.pointaccount.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePointAccountRequest(
        @NotBlank(message = "userId is required.")
        String userId
) {
}
