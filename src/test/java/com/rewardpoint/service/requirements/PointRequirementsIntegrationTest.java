package com.rewardpoint.service.requirements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import com.rewardpoint.service.pointearn.entity.PointGrant;
import com.rewardpoint.service.pointearn.entity.PointGrantType;
import com.rewardpoint.service.pointearn.service.PointEarnService;
import com.rewardpoint.service.pointearn.service.dto.CancelEarnPointCommand;
import com.rewardpoint.service.pointearn.service.dto.EarnPointCommand;
import com.rewardpoint.service.pointearn.repository.PointGrantRepository;
import com.rewardpoint.service.pointcore.entity.PointPolicy;
import com.rewardpoint.service.pointcore.entity.PointTransaction;
import com.rewardpoint.service.pointcore.exception.PointOperationException;
import com.rewardpoint.service.pointcore.repository.PointPolicyRepository;
import com.rewardpoint.service.pointcore.repository.PointTransactionRepository;
import com.rewardpoint.service.pointcore.support.dto.PointOperationResult;
import com.rewardpoint.service.pointuse.entity.PointUse;
import com.rewardpoint.service.pointuse.entity.PointUseAllocation;
import com.rewardpoint.service.pointuse.entity.PointUseCancelAllocation;
import com.rewardpoint.service.pointuse.entity.PointUseCancelRestoreType;
import com.rewardpoint.service.pointuse.repository.PointUseAllocationRepository;
import com.rewardpoint.service.pointuse.repository.PointUseCancelAllocationRepository;
import com.rewardpoint.service.pointuse.repository.PointUseRepository;
import com.rewardpoint.service.pointuse.service.PointUseCancelService;
import com.rewardpoint.service.pointuse.service.PointUseService;
import com.rewardpoint.service.pointuse.service.dto.CancelUsePointCommand;
import com.rewardpoint.service.pointuse.service.dto.UsePointCommand;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("포인트 요구사항 통합 테스트")
class PointRequirementsIntegrationTest {

    @Autowired
    private PointEarnService pointEarnService;

    @Autowired
    private PointUseService pointUseService;

    @Autowired
    private PointUseCancelService pointUseCancelService;

    @Autowired
    private PointAccountRepository pointAccountRepository;

    @Autowired
    private PointPolicyRepository pointPolicyRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private PointGrantRepository pointGrantRepository;

    @Autowired
    private PointUseRepository pointUseRepository;

    @Autowired
    private PointUseAllocationRepository pointUseAllocationRepository;

    @Autowired
    private PointUseCancelAllocationRepository pointUseCancelAllocationRepository;

    @Test
    @DisplayName("정책으로 1회 최대 적립 가능 포인트를 제어할 수 있다")
    void controlsMaxEarnAmountByPolicy() {
        PointAccount account = pointAccountRepository.save(new PointAccount("policy-max-earn-user"));
        saveActivePolicy(500L, 1_000_000L, 365, 1, 365 * 5);

        PointOperationResult successResult = pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 500L, false, null, "earn within policy")
        );

        assertThat(successResult.amount()).isEqualTo(500L);

        assertThatThrownBy(() -> pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 501L, false, null, "earn over policy")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("적립 금액은 1 이상 500 이하");
    }

    @Test
    @DisplayName("정책으로 무료 포인트 최대 보유 한도를 제어할 수 있다")
    void controlsMaxFreePointBalanceByPolicy() {
        PointAccount account = pointAccountRepository.save(new PointAccount("policy-balance-user"));
        saveActivePolicy(100_000L, 700L, 365, 1, 365 * 5);

        pointEarnService.earn(new EarnPointCommand(account.getAccountId(), 600L, false, null, "first earn"));

        assertThatThrownBy(() -> pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 101L, false, null, "second earn")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("무료 포인트 보유 한도");
    }

    @Test
    @DisplayName("수기 지급 포인트는 별도로 식별되고 기본 만료일이 적용된다")
    void distinguishesManualGrantAndAppliesDefaultExpireDays() {
        PointAccount account = pointAccountRepository.save(new PointAccount("manual-grant-user"));
        saveActivePolicy(100_000L, 1_000_000L, 365, 1, 365 * 5);
        LocalDateTime beforeEarn = LocalDateTime.now();

        PointOperationResult result = pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 1_000L, true, null, "manual earn")
        );

        PointTransaction transaction = pointTransactionRepository.findByTransactionKey(result.transactionKey()).orElseThrow();
        PointGrant grant = pointGrantRepository.findByTransaction(transaction).orElseThrow();
        LocalDateTime afterEarn = LocalDateTime.now();

        assertThat(grant.isManual()).isTrue();
        assertThat(grant.getGrantType()).isEqualTo(PointGrantType.MANUAL);
        assertThat(grant.getExpireAt()).isAfterOrEqualTo(beforeEarn.plusDays(365));
        assertThat(grant.getExpireAt()).isBeforeOrEqualTo(afterEarn.plusDays(365));
    }

    @Test
    @DisplayName("만료일은 최소 1일 이상 최대 5년 미만만 허용된다")
    void validatesExpireDaysRange() {
        PointAccount account = pointAccountRepository.save(new PointAccount("expire-days-user"));
        saveActivePolicy(100_000L, 1_000_000L, 365, 1, 365 * 5);

        assertThatThrownBy(() -> pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 100L, false, 0, "invalid min")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만료일은");

        assertThatThrownBy(() -> pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 100L, false, 365 * 5, "invalid max")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만료일은");
    }

    @Test
    @DisplayName("포인트 사용 내역은 주문번호와 allocation으로 1원 단위까지 추적할 수 있다")
    void tracksUseByOrderAndAllocation() {
        PointAccount account = pointAccountRepository.save(new PointAccount("trace-allocation-user"));

        PointOperationResult earnA = pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 1_000L, false, 30, "earn-a")
        );
        PointOperationResult earnB = pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 500L, false, 60, "earn-b")
        );

        PointOperationResult useResult = pointUseService.use(
                new UsePointCommand(account.getAccountId(), "ORDER-TRACE", 1_200L, "use")
        );

        PointTransaction useTransaction = pointTransactionRepository.findByTransactionKey(useResult.transactionKey()).orElseThrow();
        PointUse pointUse = pointUseRepository.findByTransaction(useTransaction).orElseThrow();
        List<PointUseAllocation> allocations = pointUseAllocationRepository.findByPointUseOrderByAllocationIdAsc(pointUse);

        assertThat(pointUse.getOrderNo()).isEqualTo("ORDER-TRACE");
        assertThat(allocations).hasSize(2);
        assertThat(allocations.get(0).getGrant().getTransaction().getTransactionKey()).isEqualTo(earnA.transactionKey());
        assertThat(allocations.get(0).getAllocatedAmount()).isEqualTo(1_000L);
        assertThat(allocations.get(1).getGrant().getTransaction().getTransactionKey()).isEqualTo(earnB.transactionKey());
        assertThat(allocations.get(1).getAllocatedAmount()).isEqualTo(200L);
    }

    @Test
    @DisplayName("포인트 사용 시 수기 지급이 우선되고 이후 만료일이 빠른 순서로 차감된다")
    void usesManualFirstThenEarliestExpireGrant() {
        PointAccount account = pointAccountRepository.save(new PointAccount("manual-expire-order-user"));

        PointOperationResult normalLate = pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 300L, false, 30, "normal-late")
        );
        PointOperationResult normalEarly = pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 400L, false, 10, "normal-early")
        );
        PointOperationResult manual = pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 200L, true, 100, "manual")
        );

        PointOperationResult useResult = pointUseService.use(
                new UsePointCommand(account.getAccountId(), "ORDER-PRIORITY", 500L, "use")
        );

        assertThat(useResult.lines()).hasSize(2);
        assertThat(useResult.lines().get(0).sourceTransactionKey()).isEqualTo(manual.transactionKey());
        assertThat(useResult.lines().get(0).amount()).isEqualTo(200L);
        assertThat(useResult.lines().get(1).sourceTransactionKey()).isEqualTo(normalEarly.transactionKey());
        assertThat(useResult.lines().get(1).amount()).isEqualTo(300L);
        assertThat(useResult.lines()).extracting(line -> line.sourceTransactionKey())
                .doesNotContain(normalLate.transactionKey());
    }

    @Test
    @DisplayName("일부라도 사용된 적립 포인트는 적립취소할 수 없다")
    void doesNotCancelEarnWhenPartiallyUsed() {
        PointAccount account = pointAccountRepository.save(new PointAccount("earn-cancel-block-user"));

        PointOperationResult earnResult = pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 1_000L, false, 30, "earn")
        );
        pointUseService.use(new UsePointCommand(account.getAccountId(), "ORDER-CANCEL-BLOCK", 100L, "use"));

        assertThatThrownBy(() -> pointEarnService.cancelEarn(
                new CancelEarnPointCommand(account.getAccountId(), earnResult.transactionKey(), "cancel earn")
        )).isInstanceOf(PointOperationException.class)
                .hasMessage("이미 사용된 적립 포인트는 취소할 수 없습니다.");
    }

    @Test
    @DisplayName("사용취소는 부분취소할 수 있고 만료된 금액은 신규 적립으로 복원된다")
    void supportsPartialCancelAndReissuesExpiredAmount() {
        PointAccount account = pointAccountRepository.save(new PointAccount("partial-cancel-user"));

        PointOperationResult earnA = pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 1_000L, false, 30, "earn-a")
        );
        PointOperationResult earnB = pointEarnService.earn(
                new EarnPointCommand(account.getAccountId(), 500L, false, 60, "earn-b")
        );

        PointOperationResult useResult = pointUseService.use(
                new UsePointCommand(account.getAccountId(), "A1234", 1_200L, "use")
        );

        PointTransaction earnATransaction = pointTransactionRepository.findByTransactionKey(earnA.transactionKey()).orElseThrow();
        PointGrant earnAGrant = pointGrantRepository.findByTransaction(earnATransaction).orElseThrow();
        setExpireAt(earnAGrant, LocalDateTime.now().minusDays(1));

        PointOperationResult cancelResult = pointUseCancelService.cancelUse(
                new CancelUsePointCommand(account.getAccountId(), useResult.transactionKey(), 1_100L, "cancel-use")
        );

        PointTransaction useTransaction = pointTransactionRepository.findByTransactionKey(useResult.transactionKey()).orElseThrow();
        PointUse pointUse = pointUseRepository.findByTransaction(useTransaction).orElseThrow();
        List<PointUseCancelAllocation> cancelAllocations = pointUseCancelAllocationRepository.findAll()
                .stream()
                .filter(allocation -> allocation.getPointUseCancel().getTransaction().getTransactionKey()
                        .equals(cancelResult.transactionKey()))
                .sorted(Comparator.comparing(PointUseCancelAllocation::getUseCancelAllocationId))
                .toList();

        assertThat(cancelResult.currentBalance()).isEqualTo(1_400L);
        assertThat(cancelResult.lines()).hasSize(2);
        assertThat(cancelResult.lines().get(0).sourceTransactionKey()).isEqualTo(earnA.transactionKey());
        assertThat(cancelResult.lines().get(0).amount()).isEqualTo(1_000L);
        assertThat(cancelResult.lines().get(0).restoreType()).isEqualTo(PointUseCancelRestoreType.REISSUE_NEW_LOT.name());
        assertThat(cancelResult.lines().get(0).reissuedTransactionKey()).isNotBlank();
        assertThat(cancelResult.lines().get(1).sourceTransactionKey()).isEqualTo(earnB.transactionKey());
        assertThat(cancelResult.lines().get(1).amount()).isEqualTo(100L);
        assertThat(cancelResult.lines().get(1).restoreType()).isEqualTo(PointUseCancelRestoreType.RESTORE_ORIGINAL_LOT.name());
        assertThat(pointUse.getRemainingCancelableAmount()).isEqualTo(100L);
        assertThat(cancelAllocations).hasSize(2);
        assertThat(cancelAllocations.get(0).getRestoreType()).isEqualTo(PointUseCancelRestoreType.REISSUE_NEW_LOT);
        assertThat(cancelAllocations.get(0).getReissuedGrant()).isNotNull();
        assertThat(cancelAllocations.get(1).getRestoreType()).isEqualTo(PointUseCancelRestoreType.RESTORE_ORIGINAL_LOT);
    }

    private void saveActivePolicy(
            long maxEarnAmountPerRequest,
            long maxFreePointBalance,
            int defaultExpireDays,
            int minExpireDays,
            int maxExpireDays
    ) {
        pointPolicyRepository.save(new PointPolicy(
                maxEarnAmountPerRequest,
                maxFreePointBalance,
                defaultExpireDays,
                minExpireDays,
                maxExpireDays,
                true
        ));
    }

    private void setExpireAt(PointGrant grant, LocalDateTime expireAt) {
        try {
            var field = PointGrant.class.getDeclaredField("expireAt");
            field.setAccessible(true);
            field.set(grant, expireAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
