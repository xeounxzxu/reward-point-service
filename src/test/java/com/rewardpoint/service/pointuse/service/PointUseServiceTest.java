package com.rewardpoint.service.pointuse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.exception.PointAccountNotFoundException;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import com.rewardpoint.service.pointearn.entity.PointGrant;
import com.rewardpoint.service.pointearn.entity.PointGrantType;
import com.rewardpoint.service.pointcore.entity.PointTransaction;
import com.rewardpoint.service.pointcore.entity.PointTransactionType;
import com.rewardpoint.service.pointuse.entity.PointUse;
import com.rewardpoint.service.pointuse.entity.PointUseAllocation;
import com.rewardpoint.service.pointcore.exception.PointOperationException;
import com.rewardpoint.service.pointearn.repository.PointGrantRepository;
import com.rewardpoint.service.pointcore.repository.PointTransactionRepository;
import com.rewardpoint.service.pointuse.repository.PointUseAllocationRepository;
import com.rewardpoint.service.pointuse.repository.PointUseRepository;
import com.rewardpoint.service.pointearn.support.PointExpirationService;
import com.rewardpoint.service.pointcore.support.TransactionKeyGenerator;
import com.rewardpoint.service.pointcore.support.dto.PointOperationResult;
import com.rewardpoint.service.pointuse.service.dto.UsePointCommand;
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
@DisplayName("포인트 사용 서비스 테스트")
class PointUseServiceTest {

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
    private PointExpirationService pointExpirationService;

    @Mock
    private TransactionKeyGenerator transactionKeyGenerator;

    @InjectMocks
    private PointUseService pointUseService;

    @Test
    @DisplayName("사용 요청이 성공하면 사용 거래와 allocation을 생성한다")
    void usesPointSuccessfully() {
        PointAccount account = account(1L, "use-user", 1_200L);
        PointGrant manualGrant = grant(10L, "grant-manual", account, 500L, LocalDateTime.now().plusDays(10), true);
        PointGrant normalGrant = grant(11L, "grant-normal", account, 1_000L, LocalDateTime.now().plusDays(30), false);

        given(pointAccountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(account));
        given(pointGrantRepository.findAvailableGrantsForUse(any(Long.class), any(LocalDateTime.class)))
                .willReturn(List.of(manualGrant, normalGrant));
        given(transactionKeyGenerator.generate()).willReturn("use-tx");
        given(pointTransactionRepository.save(any(PointTransaction.class))).willAnswer(invocation -> {
            PointTransaction transaction = invocation.getArgument(0);
            setField(transaction, "transactionId", 200L);
            return transaction;
        });
        given(pointUseRepository.save(any(PointUse.class))).willAnswer(invocation -> {
            PointUse pointUse = invocation.getArgument(0);
            setField(pointUse, "useId", 300L);
            return pointUse;
        });

        PointOperationResult result = pointUseService.use(
                new UsePointCommand(1L, "ORDER-1", 1_200L, "use")
        );

        assertThat(result.transactionKey()).isEqualTo("use-tx");
        assertThat(result.currentBalance()).isZero();
        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines().get(0).sourceTransactionKey()).isEqualTo("grant-manual");
        assertThat(result.lines().get(1).sourceTransactionKey()).isEqualTo("grant-normal");
        assertThat(manualGrant.getRemainingAmount()).isZero();
        assertThat(normalGrant.getRemainingAmount()).isEqualTo(300L);
        verify(pointUseAllocationRepository, times(2)).save(any(PointUseAllocation.class));
    }

    @Test
    @DisplayName("잔액이 부족하면 사용할 수 없다")
    void throwsWhenBalanceIsInsufficient() {
        PointAccount account = account(1L, "use-user", 100L);
        given(pointAccountRepository.findByIdForUpdate(1L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> pointUseService.use(new UsePointCommand(1L, "ORDER-1", 200L, "use")))
                .isInstanceOf(PointOperationException.class)
                .hasMessage("사용 가능한 포인트가 부족합니다.");
    }

    @Test
    @DisplayName("존재하지 않는 계정으로 사용하면 예외가 발생한다")
    void throwsWhenAccountMissingOnUse() {
        given(pointAccountRepository.findByIdForUpdate(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pointUseService.use(new UsePointCommand(99L, "ORDER-1", 100L, "use")))
                .isInstanceOf(PointAccountNotFoundException.class)
                .hasMessageContaining("99");
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
