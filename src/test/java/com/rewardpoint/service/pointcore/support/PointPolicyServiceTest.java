package com.rewardpoint.service.pointcore.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.rewardpoint.service.pointcore.entity.PointPolicy;
import com.rewardpoint.service.pointcore.exception.PointPolicyNotFoundException;
import com.rewardpoint.service.pointcore.repository.PointPolicyRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("포인트 정책 서비스 테스트")
class PointPolicyServiceTest {

    @Mock
    private PointPolicyRepository pointPolicyRepository;

    @InjectMocks
    private PointPolicyService pointPolicyService;

    @Test
    @DisplayName("활성 정책을 조회할 수 있다")
    void getsActivePolicy() {
        PointPolicy policy = new PointPolicy(100_000L, 1_000_000L, 365, 1, 1825, true);
        given(pointPolicyRepository.findFirstByActiveTrueOrderByPolicyIdDesc()).willReturn(Optional.of(policy));

        PointPolicy result = pointPolicyService.getActivePolicy();

        assertThat(result).isSameAs(policy);
    }

    @Test
    @DisplayName("활성 정책이 없으면 예외가 발생한다")
    void throwsWhenActivePolicyMissing() {
        given(pointPolicyRepository.findFirstByActiveTrueOrderByPolicyIdDesc()).willReturn(Optional.empty());

        assertThatThrownBy(() -> pointPolicyService.getActivePolicy())
                .isInstanceOf(PointPolicyNotFoundException.class);
    }
}
