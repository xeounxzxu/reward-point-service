package com.rewardpoint.service.point.config;

import com.rewardpoint.service.pointledger.entity.PointPolicy;
import com.rewardpoint.service.pointledger.repository.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PointPolicyBootstrap {

    private final PointPolicyRepository pointPolicyRepository;

    @Bean
    public ApplicationRunner pointPolicyInitializer() {
        return args -> {
            if (pointPolicyRepository.findFirstByActiveTrueOrderByPolicyIdDesc().isEmpty()) {
                pointPolicyRepository.save(new PointPolicy(
                        100_000L,
                        1_000_000L,
                        365,
                        1,
                        365 * 5,
                        true
                ));
            }
        };
    }
}
