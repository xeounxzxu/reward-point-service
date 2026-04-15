package com.rewardpoint.service.pointaccount.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.rewardpoint.service.global.config.JpaAuditingConfig;
import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.entity.PointAccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@DisplayName("포인트 계정 리포지토리 테스트")
class PointAccountRepositoryTest {

    @Autowired
    private PointAccountRepository pointAccountRepository;

    @Test
    @DisplayName("포인트 계정을 저장하면 기본 상태와 감사 컬럼이 저장된다")
    void savesPointAccount() {
        PointAccount pointAccount = new PointAccount("user-100");

        PointAccount saved = pointAccountRepository.save(pointAccount);

        assertThat(saved.getAccountId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo("user-100");
        assertThat(saved.getStatus()).isEqualTo(PointAccountStatus.ACTIVE);
        assertThat(saved.getCurrentBalance()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("userId로 포인트 계정 존재 여부와 단건 조회가 가능하다")
    void findsPointAccountByUserId() {
        pointAccountRepository.save(new PointAccount("user-find"));

        boolean exists = pointAccountRepository.existsByUserId("user-find");
        PointAccount found = pointAccountRepository.findByUserId("user-find").orElseThrow();

        assertThat(exists).isTrue();
        assertThat(found.getUserId()).isEqualTo("user-find");
    }
}
