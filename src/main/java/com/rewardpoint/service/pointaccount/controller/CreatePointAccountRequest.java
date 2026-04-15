package com.rewardpoint.service.pointaccount.controller;

import jakarta.validation.constraints.NotBlank;

public record CreatePointAccountRequest(
        @NotBlank(message = "userId는 필수입니다.")
        String userId
) {
}
