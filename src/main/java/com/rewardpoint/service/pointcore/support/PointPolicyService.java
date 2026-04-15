package com.rewardpoint.service.pointcore.support;

import com.rewardpoint.service.pointcore.entity.PointPolicy;
import com.rewardpoint.service.pointcore.exception.PointPolicyNotFoundException;
import com.rewardpoint.service.pointcore.repository.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointPolicyService {

    private final PointPolicyRepository pointPolicyRepository;

    public PointPolicy getActivePolicy() {
        return pointPolicyRepository.findFirstByActiveTrueOrderByPolicyIdDesc()
                .orElseThrow(PointPolicyNotFoundException::new);
    }
}
