package com.rewardpoint.service.pointaccount.controller;

import com.rewardpoint.service.pointaccount.entity.PointAccountStatus;
import com.rewardpoint.service.pointaccount.service.PointAccountResult;
import java.time.LocalDateTime;

public record PointAccountResponse(
        Long accountId,
        String userId,
        PointAccountStatus status,
        long currentBalance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PointAccountResponse from(PointAccountResult result) {
        return new PointAccountResponse(
                result.accountId(),
                result.userId(),
                result.status(),
                result.currentBalance(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
