package com.rewardpoint.service.pointuse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import com.rewardpoint.service.pointearn.entity.PointGrant;
import com.rewardpoint.service.pointearn.entity.PointGrantType;
import com.rewardpoint.service.pointearn.repository.PointGrantRepository;
import com.rewardpoint.service.pointearn.support.PointExpirationService;
import com.rewardpoint.service.pointcore.entity.PointPolicy;
import com.rewardpoint.service.pointcore.entity.PointTransaction;
import com.rewardpoint.service.pointcore.entity.PointTransactionType;
import com.rewardpoint.service.pointcore.exception.PointOperationException;
import com.rewardpoint.service.pointcore.repository.PointTransactionRepository;
import com.rewardpoint.service.pointcore.support.PointPolicyService;
import com.rewardpoint.service.pointcore.support.TransactionKeyGenerator;
import com.rewardpoint.service.pointcore.support.dto.PointOperationResult;
import com.rewardpoint.service.pointuse.entity.PointUse;
import com.rewardpoint.service.pointuse.entity.PointUseAllocation;
import com.rewardpoint.service.pointuse.entity.PointUseCancel;
import com.rewardpoint.service.pointuse.entity.PointUseCancelAllocation;
import com.rewardpoint.service.pointuse.entity.PointUseCancelRestoreType;
import com.rewardpoint.service.pointuse.repository.PointUseAllocationRepository;
import com.rewardpoint.service.pointuse.repository.PointUseCancelAllocationRepository;
import com.rewardpoint.service.pointuse.repository.PointUseCancelRepository;
import com.rewardpoint.service.pointuse.repository.PointUseRepository;
import com.rewardpoint.service.pointuse.service.dto.CancelUsePointCommand;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("포인트 사용취소 서비스 테스트")
class PointUseCancelServiceTest {

    @Mock
    private PointAccountRepository pointAccountRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private PointGrantRepository pointGrantRepository;

    @Mock
    private PointUseRepository pointUseRepository;

    @Mock
    private PointUseAllocationRepository pointUseAllocationRepository;

    @Mock
    private PointUseCancelRepository pointUseCancelRepository;

    @Mock
    private PointUseCancelAllocationRepository pointUseCancelAllocationRepository;

    @Mock
    private PointPolicyService pointPolicyService;

    @Mock
    private PointExpirationService pointExpirationService;

    @Mock
    private TransactionKeyGenerator transactionKeyGenerator;

    @InjectMocks
    private PointUseCancelService pointUseCancelService;

    @Test
    @DisplayName("사용취소 시 만료된 지급 건은 재적립으로 복원된다")
    void cancelsUseWithReissueSuccessfully() {
        PointAccount account = account(1L, "cancel-user", 0L);
        PointTransaction useTransaction = transaction(100L, "use-target", account, PointTransactionType.USE, 500L, null, "ORDER-1");
        PointUse pointUse = new PointUse(useTransaction, account, "ORDER-1", 500L);
        PointGrant expiredGrant = grant(10L, "expired-grant", account, 500L, LocalDateTime.now().minusDays(1), false);
        PointUseAllocation allocation = new PointUseAllocation(pointUse, expiredGrant, 500L);

        given(pointAccountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(account));
        given(pointTransactionRepository.findByTransactionKey("use-target")).willReturn(Optional.of(useTransaction));
        given(pointUseRepository.findByTransaction(useTransaction)).willReturn(Optional.of(pointUse));
        given(pointUseAllocationRepository.findByPointUseOrderByAllocationIdAsc(pointUse)).willReturn(List.of(allocation));
        given(transactionKeyGenerator.generate()).willReturn("cancel-use-tx", "reissue-tx");
        given(pointTransactionRepository.save(any(PointTransaction.class))).willAnswer(invocation -> {
            PointTransaction transaction = invocation.getArgument(0);
            if (transaction.getTransactionId() == null) {
                setField(transaction, "transactionId", Math.abs(transaction.getTransactionKey().hashCode()) + 1L);
            }
            return transaction;
        });
        given(pointUseCancelRepository.save(any(PointUseCancel.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(pointGrantRepository.save(any(PointGrant.class))).willAnswer(invocation -> {
            PointGrant grant = invocation.getArgument(0);
            setField(grant, "grantId", 20L);
            return grant;
        });
        given(pointPolicyService.getActivePolicy()).willReturn(new PointPolicy(
                100_000L,
                1_000_000L,
                30,
                1,
                365,
                true
        ));

        PointOperationResult result = pointUseCancelService.cancelUse(
                new CancelUsePointCommand(1L, "use-target", 500L, "cancel")
        );

        assertThat(result.transactionKey()).isEqualTo("cancel-use-tx");
        assertThat(result.currentBalance()).isEqualTo(500L);
        assertThat(result.lines()).singleElement().satisfies(line -> {
            assertThat(line.sourceTransactionKey()).isEqualTo("expired-grant");
            assertThat(line.restoreType()).isEqualTo(PointUseCancelRestoreType.REISSUE_NEW_LOT.name());
            assertThat(line.reissuedTransactionKey()).isEqualTo("reissue-tx");
        });
        verify(pointUseCancelAllocationRepository).save(any(PointUseCancelAllocation.class));
    }

    @Test
    @DisplayName("남은 취소 가능 금액을 초과하면 사용취소할 수 없다")
    void throwsWhenCancelAmountExceedsRemaining() {
        PointAccount account = account(1L, "cancel-user", 0L);
        PointTransaction useTransaction = transaction(100L, "use-target", account, PointTransactionType.USE, 500L, null, "ORDER-1");
        PointUse pointUse = new PointUse(useTransaction, account, "ORDER-1", 500L);
        pointUse.cancel(400L);

        given(pointAccountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(account));
        given(pointTransactionRepository.findByTransactionKey("use-target")).willReturn(Optional.of(useTransaction));
        given(pointUseRepository.findByTransaction(useTransaction)).willReturn(Optional.of(pointUse));

        assertThatThrownBy(() -> pointUseCancelService.cancelUse(
                new CancelUsePointCommand(1L, "use-target", 200L, "cancel")
        )).isInstanceOf(PointOperationException.class)
                .hasMessage("사용취소 금액이 남은 취소 가능 금액을 초과할 수 없습니다.");
    }

    private PointAccount account(Long accountId, String userId, long balance) {
        PointAccount account = new PointAccount(userId);
        setField(account, "accountId", accountId);
        setField(account, "currentBalance", balance);
        return account;
    }

    private PointTransaction transaction(
            Long transactionId,
            String transactionKey,
            PointAccount account,
            PointTransactionType transactionType,
            long amount,
            Long originTransactionId,
            String orderNo
    ) {
        PointTransaction transaction = new PointTransaction(
                transactionKey,
                account,
                transactionType,
                amount,
                originTransactionId,
                orderNo,
                "desc"
        );
        setField(transaction, "transactionId", transactionId);
        return transaction;
    }

    private PointGrant grant(
            Long grantId,
            String transactionKey,
            PointAccount account,
            long amount,
            LocalDateTime expireAt,
            boolean manual
    ) {
        PointTransaction transaction = transaction(
                grantId + 100L,
                transactionKey,
                account,
                PointTransactionType.EARN,
                amount,
                null,
                null
        );
        PointGrant grant = new PointGrant(
                transaction,
                account,
                manual ? PointGrantType.MANUAL : PointGrantType.NORMAL,
                amount,
                expireAt,
                manual
        );
        setField(grant, "grantId", grantId);
        return grant;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
