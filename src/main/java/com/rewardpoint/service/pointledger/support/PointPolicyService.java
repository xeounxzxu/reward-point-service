package com.rewardpoint.service.pointledger.support;

import com.rewardpoint.service.pointledger.entity.PointPolicy;
import com.rewardpoint.service.pointledger.exception.PointPolicyNotFoundException;
import com.rewardpoint.service.pointledger.repository.PointPolicyRepository;
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
