package com.rewardpoint.service.pointledger.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointledger.entity.PointExpirationHistory;
import com.rewardpoint.service.pointledger.entity.PointGrant;
import com.rewardpoint.service.pointledger.entity.PointGrantType;
import com.rewardpoint.service.pointledger.entity.PointTransaction;
import com.rewardpoint.service.pointledger.entity.PointTransactionType;
import com.rewardpoint.service.pointledger.repository.PointExpirationHistoryRepository;
import com.rewardpoint.service.pointledger.repository.PointGrantRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("포인트 만료 서비스 테스트")
class PointExpirationServiceTest {

    @Mock
    private PointGrantRepository pointGrantRepository;

    @Mock
    private PointExpirationHistoryRepository pointExpirationHistoryRepository;

    @InjectMocks
    private PointExpirationService pointExpirationService;

    @Test
    @DisplayName("만료 대상 지급 건이 있으면 잔액 차감과 만료 이력을 남긴다")
    void expiresAvailableGrants() {
        PointAccount account = new PointAccount("expire-user");
        setField(account, "accountId", 1L);
        setField(account, "currentBalance", 700L);

        PointGrant expiredGrant = new PointGrant(
                new PointTransaction("tx-expire", account, PointTransactionType.EARN, 500L, null, null, "expire"),
                account,
                PointGrantType.NORMAL,
                500L,
                LocalDateTime.now().minusDays(1),
                false
        );

        given(pointGrantRepository.findExpiredGrants(any(Long.class), any(LocalDateTime.class)))
                .willReturn(List.of(expiredGrant));

        pointExpirationService.expire(account, LocalDateTime.now());

        assertThat(account.getCurrentBalance()).isEqualTo(200L);
        assertThat(expiredGrant.getRemainingAmount()).isZero();
        verify(pointExpirationHistoryRepository).save(any(PointExpirationHistory.class));
    }

    @Test
    @DisplayName("남은 금액이 없는 지급 건은 만료 이력을 남기지 않는다")
    void skipsExpiredGrantWithoutRemainingAmount() {
        PointAccount account = new PointAccount("expire-user");
        setField(account, "accountId", 1L);
        setField(account, "currentBalance", 200L);

        PointGrant expiredGrant = new PointGrant(
                new PointTransaction("tx-expire", account, PointTransactionType.EARN, 100L, null, null, "expire"),
                account,
                PointGrantType.NORMAL,
                100L,
                LocalDateTime.now().minusDays(1),
                false
        );
        expiredGrant.use(100L);

        given(pointGrantRepository.findExpiredGrants(any(Long.class), any(LocalDateTime.class)))
                .willReturn(List.of(expiredGrant));

        pointExpirationService.expire(account, LocalDateTime.now());

        assertThat(account.getCurrentBalance()).isEqualTo(200L);
        verify(pointExpirationHistoryRepository, never()).save(any(PointExpirationHistory.class));
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
