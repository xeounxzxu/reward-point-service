package com.rewardpoint.service.pointearn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.exception.PointAccountNotFoundException;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import com.rewardpoint.service.pointledger.entity.PointGrant;
import com.rewardpoint.service.pointledger.entity.PointGrantCancel;
import com.rewardpoint.service.pointledger.entity.PointGrantType;
import com.rewardpoint.service.pointledger.entity.PointPolicy;
import com.rewardpoint.service.pointledger.entity.PointTransaction;
import com.rewardpoint.service.pointledger.entity.PointTransactionType;
import com.rewardpoint.service.pointledger.exception.PointOperationException;
import com.rewardpoint.service.pointledger.exception.PointTransactionNotFoundException;
import com.rewardpoint.service.pointledger.repository.PointGrantCancelRepository;
import com.rewardpoint.service.pointledger.repository.PointGrantRepository;
import com.rewardpoint.service.pointledger.repository.PointTransactionRepository;
import com.rewardpoint.service.pointledger.support.PointExpirationService;
import com.rewardpoint.service.pointledger.support.PointPolicyService;
import com.rewardpoint.service.pointledger.support.TransactionKeyGenerator;
import com.rewardpoint.service.pointledger.support.dto.PointOperationResult;
import com.rewardpoint.service.pointearn.service.dto.CancelEarnPointCommand;
import com.rewardpoint.service.pointearn.service.dto.EarnPointCommand;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("포인트 적립 서비스 테스트")
class PointEarnServiceTest {

    @Mock
    private PointAccountRepository pointAccountRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private PointGrantRepository pointGrantRepository;

    @Mock
    private PointGrantCancelRepository pointGrantCancelRepository;

    @Mock
    private PointPolicyService pointPolicyService;

    @Mock
    private PointExpirationService pointExpirationService;

    @Mock
    private TransactionKeyGenerator transactionKeyGenerator;

    @InjectMocks
    private PointEarnService pointEarnService;

    @Test
    @DisplayName("적립 요청이 성공하면 거래와 지급 건을 생성하고 잔액을 증가시킨다")
    void earnsPointSuccessfully() {
        PointAccount account = account(1L, "earn-user", 100L);
        PointPolicy policy = new PointPolicy(100_000L, 1_000_000L, 30, 1, 365, true);

        given(pointAccountRepository.findById(1L)).willReturn(Optional.of(account));
        given(pointPolicyService.getActivePolicy()).willReturn(policy);
        given(transactionKeyGenerator.generate()).willReturn("tx-earn");
        given(pointTransactionRepository.save(any(PointTransaction.class))).willAnswer(invocation -> {
            PointTransaction transaction = invocation.getArgument(0);
            setField(transaction, "transactionId", 10L);
            return transaction;
        });
        given(pointGrantRepository.save(any(PointGrant.class))).willAnswer(invocation -> {
            PointGrant grant = invocation.getArgument(0);
            setField(grant, "grantId", 20L);
            return grant;
        });

        PointOperationResult result = pointEarnService.earn(
                new EarnPointCommand(1L, 500L, true, 10, "manual earn")
        );

        assertThat(result.transactionKey()).isEqualTo("tx-earn");
        assertThat(result.transactionType()).isEqualTo(PointTransactionType.EARN);
        assertThat(result.currentBalance()).isEqualTo(600L);
        assertThat(result.lines()).singleElement().satisfies(line -> {
            assertThat(line.sourceTransactionKey()).isEqualTo("tx-earn");
            assertThat(line.amount()).isEqualTo(500L);
        });
        assertThat(account.getCurrentBalance()).isEqualTo(600L);
        verify(pointExpirationService).expire(any(PointAccount.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("존재하지 않는 계정으로 적립하면 예외가 발생한다")
    void throwsWhenAccountMissingOnEarn() {
        given(pointAccountRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pointEarnService.earn(new EarnPointCommand(99L, 100L, false, 30, "earn")))
                .isInstanceOf(PointAccountNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("무료 포인트 보유 한도를 초과하면 적립할 수 없다")
    void throwsWhenBalanceLimitExceededOnEarn() {
        PointAccount account = account(1L, "earn-user", 950L);
        PointPolicy policy = new PointPolicy(100_000L, 1_000L, 30, 1, 365, true);

        given(pointAccountRepository.findById(1L)).willReturn(Optional.of(account));
        given(pointPolicyService.getActivePolicy()).willReturn(policy);

        assertThatThrownBy(() -> pointEarnService.earn(new EarnPointCommand(1L, 100L, false, 30, "earn")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("무료 포인트 보유 한도");

        verify(pointTransactionRepository, never()).save(any(PointTransaction.class));
    }

    @Test
    @DisplayName("정책 범위를 벗어난 만료일로 적립하면 예외가 발생한다")
    void throwsWhenExpireDaysOutOfPolicyRange() {
        PointAccount account = account(1L, "earn-user", 100L);
        PointPolicy policy = new PointPolicy(100_000L, 1_000_000L, 30, 1, 365, true);

        given(pointAccountRepository.findById(1L)).willReturn(Optional.of(account));
        given(pointPolicyService.getActivePolicy()).willReturn(policy);

        assertThatThrownBy(() -> pointEarnService.earn(new EarnPointCommand(1L, 100L, false, 365, "earn")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만료일은");

        verify(pointTransactionRepository, never()).save(any(PointTransaction.class));
    }

    @Test
    @DisplayName("미사용 적립 건은 적립취소할 수 있다")
    void cancelsEarnSuccessfully() {
        PointAccount account = account(1L, "cancel-user", 500L);
        PointTransaction targetTransaction = transaction(100L, "target-tx", account, PointTransactionType.EARN, 500L, null, null);
        PointGrant targetGrant = new PointGrant(
                targetTransaction,
                account,
                PointGrantType.NORMAL,
                500L,
                LocalDateTime.now().plusDays(30),
                false
        );

        given(pointAccountRepository.findById(1L)).willReturn(Optional.of(account));
        given(pointTransactionRepository.findByTransactionKey("target-tx")).willReturn(Optional.of(targetTransaction));
        given(pointGrantRepository.findByTransaction(targetTransaction)).willReturn(Optional.of(targetGrant));
        given(transactionKeyGenerator.generate()).willReturn("cancel-tx");
        given(pointTransactionRepository.save(any(PointTransaction.class))).willAnswer(invocation -> {
            PointTransaction transaction = invocation.getArgument(0);
            setField(transaction, "transactionId", 101L);
            return transaction;
        });

        PointOperationResult result = pointEarnService.cancelEarn(
                new CancelEarnPointCommand(1L, "target-tx", "cancel earn")
        );

        assertThat(result.transactionKey()).isEqualTo("cancel-tx");
        assertThat(result.transactionType()).isEqualTo(PointTransactionType.EARN_CANCEL);
        assertThat(result.currentBalance()).isZero();
        assertThat(targetGrant.getRemainingAmount()).isZero();
        verify(pointGrantCancelRepository).save(any(PointGrantCancel.class));
    }

    @Test
    @DisplayName("이미 사용된 적립 건은 적립취소할 수 없다")
    void throwsWhenGrantAlreadyUsedOnCancelEarn() {
        PointAccount account = account(1L, "cancel-user", 500L);
        PointTransaction targetTransaction = transaction(100L, "target-tx", account, PointTransactionType.EARN, 500L, null, null);
        PointGrant targetGrant = new PointGrant(
                targetTransaction,
                account,
                PointGrantType.NORMAL,
                500L,
                LocalDateTime.now().plusDays(30),
                false
        );
        targetGrant.use(100L);

        given(pointAccountRepository.findById(1L)).willReturn(Optional.of(account));
        given(pointTransactionRepository.findByTransactionKey("target-tx")).willReturn(Optional.of(targetTransaction));
        given(pointGrantRepository.findByTransaction(targetTransaction)).willReturn(Optional.of(targetGrant));

        assertThatThrownBy(() -> pointEarnService.cancelEarn(new CancelEarnPointCommand(1L, "target-tx", "cancel earn")))
                .isInstanceOf(PointOperationException.class)
                .hasMessage("이미 사용된 적립 포인트는 취소할 수 없습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 거래 키로 적립취소하면 예외가 발생한다")
    void throwsWhenTransactionMissingOnCancelEarn() {
        PointAccount account = account(1L, "cancel-user", 500L);
        given(pointAccountRepository.findById(1L)).willReturn(Optional.of(account));
        given(pointTransactionRepository.findByTransactionKey("missing-tx")).willReturn(Optional.empty());

        assertThatThrownBy(() -> pointEarnService.cancelEarn(new CancelEarnPointCommand(1L, "missing-tx", "cancel earn")))
                .isInstanceOf(PointTransactionNotFoundException.class)
                .hasMessageContaining("missing-tx");
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
