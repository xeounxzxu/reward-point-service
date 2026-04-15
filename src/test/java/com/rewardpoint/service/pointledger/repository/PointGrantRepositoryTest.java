package com.rewardpoint.service.pointledger.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.rewardpoint.service.global.config.JpaAuditingConfig;
import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import com.rewardpoint.service.pointledger.entity.PointGrant;
import com.rewardpoint.service.pointledger.entity.PointGrantType;
import com.rewardpoint.service.pointledger.entity.PointTransaction;
import com.rewardpoint.service.pointledger.entity.PointTransactionType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@DisplayName("포인트 지급 리포지토리 테스트")
class PointGrantRepositoryTest {

    @Autowired
    private PointGrantRepository pointGrantRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private PointAccountRepository pointAccountRepository;

    @Test
    @DisplayName("사용 가능한 지급 건은 수기 우선, 만료일 오름차순으로 조회된다")
    void findsAvailableGrantsForUseInExpectedOrder() {
        PointAccount account = pointAccountRepository.save(new PointAccount("grant-order-user"));
        LocalDateTime now = LocalDateTime.now();

        PointGrant normalEarly = saveGrant(account, "tx-normal-early", 300L, now.plusDays(3), false);
        PointGrant manualLate = saveGrant(account, "tx-manual-late", 200L, now.plusDays(10), true);
        PointGrant expired = saveGrant(account, "tx-expired", 100L, now.minusDays(1), false);
        PointGrant empty = saveGrant(account, "tx-empty", 100L, now.plusDays(5), false);
        empty.use(100L);
        pointGrantRepository.saveAndFlush(empty);

        List<PointGrant> grants = pointGrantRepository.findAvailableGrantsForUse(account.getAccountId(), now);

        assertThat(grants).extracting(grant -> grant.getTransaction().getTransactionKey())
                .containsExactly("tx-manual-late", "tx-normal-early");
        assertThat(grants).doesNotContain(expired, empty);
    }

    @Test
    @DisplayName("만료 대상 지급 건은 만료일 오름차순으로 조회된다")
    void findsExpiredGrants() {
        PointAccount account = pointAccountRepository.save(new PointAccount("grant-expire-user"));
        LocalDateTime now = LocalDateTime.now();

        saveGrant(account, "tx-expired-1", 100L, now.minusDays(3), false);
        saveGrant(account, "tx-expired-2", 100L, now.minusDays(1), false);
        saveGrant(account, "tx-active", 100L, now.plusDays(1), false);

        List<PointGrant> expiredGrants = pointGrantRepository.findExpiredGrants(account.getAccountId(), now);

        assertThat(expiredGrants).extracting(grant -> grant.getTransaction().getTransactionKey())
                .containsExactly("tx-expired-1", "tx-expired-2");
    }

    private PointGrant saveGrant(
            PointAccount account,
            String transactionKey,
            long amount,
            LocalDateTime expireAt,
            boolean manual
    ) {
        PointTransaction transaction = pointTransactionRepository.save(new PointTransaction(
                transactionKey,
                account,
                PointTransactionType.EARN,
                amount,
                null,
                null,
                transactionKey
        ));
        return pointGrantRepository.save(new PointGrant(
                transaction,
                account,
                manual ? PointGrantType.MANUAL : PointGrantType.NORMAL,
                amount,
                expireAt,
                manual
        ));
    }
}
