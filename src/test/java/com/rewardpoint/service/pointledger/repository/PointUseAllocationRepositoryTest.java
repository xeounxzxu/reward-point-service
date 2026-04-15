package com.rewardpoint.service.pointledger.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.rewardpoint.service.global.config.JpaAuditingConfig;
import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import com.rewardpoint.service.pointledger.entity.PointGrant;
import com.rewardpoint.service.pointledger.entity.PointGrantType;
import com.rewardpoint.service.pointledger.entity.PointTransaction;
import com.rewardpoint.service.pointledger.entity.PointTransactionType;
import com.rewardpoint.service.pointledger.entity.PointUse;
import com.rewardpoint.service.pointledger.entity.PointUseAllocation;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@DisplayName("포인트 사용 allocation 리포지토리 테스트")
class PointUseAllocationRepositoryTest {

    @Autowired
    private PointAccountRepository pointAccountRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private PointGrantRepository pointGrantRepository;

    @Autowired
    private PointUseRepository pointUseRepository;

    @Autowired
    private PointUseAllocationRepository pointUseAllocationRepository;

    @Test
    @DisplayName("allocation은 생성 순서대로 조회된다")
    void findsUseAllocationsInCreationOrder() {
        PointAccount account = pointAccountRepository.save(new PointAccount("allocation-user"));
        PointGrant grantA = saveGrant(account, "grant-a");
        PointGrant grantB = saveGrant(account, "grant-b");
        PointUse pointUse = saveUse(account, "use-tx");

        PointUseAllocation second = pointUseAllocationRepository.save(new PointUseAllocation(pointUse, grantB, 200L));
        PointUseAllocation first = pointUseAllocationRepository.save(new PointUseAllocation(pointUse, grantA, 300L));

        List<PointUseAllocation> allocations = pointUseAllocationRepository.findByPointUseOrderByAllocationIdAsc(pointUse);

        assertThat(allocations).extracting(PointUseAllocation::getAllocationId)
                .containsExactly(second.getAllocationId(), first.getAllocationId());
    }

    private PointGrant saveGrant(PointAccount account, String transactionKey) {
        PointTransaction transaction = pointTransactionRepository.save(new PointTransaction(
                transactionKey,
                account,
                PointTransactionType.EARN,
                500L,
                null,
                null,
                transactionKey
        ));
        return pointGrantRepository.save(new PointGrant(
                transaction,
                account,
                PointGrantType.NORMAL,
                500L,
                LocalDateTime.now().plusDays(10),
                false
        ));
    }

    private PointUse saveUse(PointAccount account, String transactionKey) {
        PointTransaction transaction = pointTransactionRepository.save(new PointTransaction(
                transactionKey,
                account,
                PointTransactionType.USE,
                500L,
                null,
                "ORDER-1",
                transactionKey
        ));
        return pointUseRepository.save(new PointUse(transaction, account, "ORDER-1", 500L));
    }
}
