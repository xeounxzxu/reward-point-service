package com.rewardpoint.service.pointearn.support;

import com.rewardpoint.service.pointearn.entity.PointGrant;
import com.rewardpoint.service.pointearn.entity.PointExpirationHistory;
import com.rewardpoint.service.pointearn.repository.PointGrantRepository;
import com.rewardpoint.service.pointearn.repository.PointExpirationHistoryRepository;
import com.rewardpoint.service.pointaccount.entity.PointAccount;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointExpirationService {

    private final PointGrantRepository pointGrantRepository;
    private final PointExpirationHistoryRepository pointExpirationHistoryRepository;

    public void expire(PointAccount account, LocalDateTime now) {
        List<PointGrant> expiredGrants = pointGrantRepository.findExpiredGrants(account.getAccountId(), now);

        for (PointGrant grant : expiredGrants) {
            long expiredAmount = grant.getRemainingAmount();
            if (expiredAmount <= 0) {
                continue;
            }

            grant.expireAll();
            account.use(expiredAmount);

            PointExpirationHistory pointExpirationHistory = new PointExpirationHistory(grant, expiredAmount, now);
            pointExpirationHistoryRepository.save(pointExpirationHistory);
        }
    }
}
