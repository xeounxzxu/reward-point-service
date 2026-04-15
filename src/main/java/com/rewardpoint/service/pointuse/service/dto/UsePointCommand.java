package com.rewardpoint.service.pointuse.service.dto;

public record UsePointCommand(
        Long accountId,
        String orderNo,
        long amount,
        String description
) {
}
