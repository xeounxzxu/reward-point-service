package com.rewardpoint.service.pointaccount.service;

import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.exception.PointAccountAlreadyExistsException;
import com.rewardpoint.service.pointaccount.exception.PointAccountNotFoundException;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import com.rewardpoint.service.pointaccount.service.dto.CreatePointAccountCommand;
import com.rewardpoint.service.pointaccount.service.dto.PointAccountResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointAccountService {

    private final PointAccountRepository pointAccountRepository;

    @Transactional
    public PointAccountResult create(CreatePointAccountCommand command) {
        if (pointAccountRepository.existsByUserId(command.userId())) {
            throw new PointAccountAlreadyExistsException(command.userId());
        }

        PointAccount saved = pointAccountRepository.save(new PointAccount(command.userId()));
        return PointAccountResult.from(saved);
    }

    public PointAccountResult getByUserId(String userId) {
        PointAccount pointAccount = pointAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new PointAccountNotFoundException(userId));
        return PointAccountResult.from(pointAccount);
    }
}
