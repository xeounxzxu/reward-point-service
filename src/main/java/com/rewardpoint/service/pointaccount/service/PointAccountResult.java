package com.rewardpoint.service.pointaccount.service;

import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.entity.PointAccountStatus;
import java.time.LocalDateTime;

public record PointAccountResult(
        Long accountId,
        String userId,
        PointAccountStatus status,
        long currentBalance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PointAccountResult from(PointAccount pointAccount) {
        return new PointAccountResult(
                pointAccount.getAccountId(),
                pointAccount.getUserId(),
                pointAccount.getStatus(),
                pointAccount.getCurrentBalance(),
                pointAccount.getCreatedAt(),
                pointAccount.getUpdatedAt()
        );
    }
}
