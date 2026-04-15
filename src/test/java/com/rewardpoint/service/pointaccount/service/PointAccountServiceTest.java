package com.rewardpoint.service.pointaccount.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.exception.PointAccountAlreadyExistsException;
import com.rewardpoint.service.pointaccount.exception.PointAccountNotFoundException;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import com.rewardpoint.service.pointaccount.service.CreatePointAccountCommand;
import com.rewardpoint.service.pointaccount.service.PointAccountResult;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("포인트 계정 서비스 테스트")
class PointAccountServiceTest {

    @Mock
    private PointAccountRepository pointAccountRepository;

    @InjectMocks
    private PointAccountService pointAccountService;

    @Test
    @DisplayName("중복되지 않은 userId면 포인트 계정을 생성한다")
    void createsPointAccountWhenUserIdDoesNotExist() {
        PointAccount pointAccount = new PointAccount("user-service");

        given(pointAccountRepository.existsByUserId("user-service")).willReturn(false);
        given(pointAccountRepository.save(any(PointAccount.class))).willReturn(pointAccount);

        PointAccountResult result = pointAccountService.create(new CreatePointAccountCommand("user-service"));

        assertThat(result.userId()).isEqualTo("user-service");
        assertThat(result.status()).isNotNull();
        verify(pointAccountRepository).save(any(PointAccount.class));
    }

    @Test
    @DisplayName("이미 존재하는 userId로 생성하면 예외가 발생한다")
    void throwsExceptionWhenPointAccountAlreadyExists() {
        given(pointAccountRepository.existsByUserId("duplicate-user")).willReturn(true);

        assertThatThrownBy(() -> pointAccountService.create(new CreatePointAccountCommand("duplicate-user")))
                .isInstanceOf(PointAccountAlreadyExistsException.class)
                .hasMessageContaining("duplicate-user");
    }

    @Test
    @DisplayName("accountId로 포인트 계정을 조회할 수 있다")
    void getsPointAccountById() {
        PointAccount pointAccount = new PointAccount("user-by-id");
        setAccountId(pointAccount, 10L);

        given(pointAccountRepository.findById(10L)).willReturn(Optional.of(pointAccount));

        PointAccountResult result = pointAccountService.getById(10L);

        assertThat(result.accountId()).isEqualTo(10L);
        assertThat(result.userId()).isEqualTo("user-by-id");
    }

    @Test
    @DisplayName("존재하지 않는 accountId로 조회하면 예외가 발생한다")
    void throwsExceptionWhenPointAccountIsMissing() {
        given(pointAccountRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pointAccountService.getById(999L))
                .isInstanceOf(PointAccountNotFoundException.class)
                .hasMessageContaining("999");
    }

    private void setAccountId(PointAccount pointAccount, Long accountId) {
        try {
            var field = PointAccount.class.getDeclaredField("accountId");
            field.setAccessible(true);
            field.set(pointAccount, accountId);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
