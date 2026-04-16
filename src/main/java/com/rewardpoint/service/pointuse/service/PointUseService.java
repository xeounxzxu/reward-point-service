package com.rewardpoint.service.pointuse.service;

import com.rewardpoint.service.pointearn.entity.PointGrant;
import com.rewardpoint.service.pointcore.entity.PointTransaction;
import com.rewardpoint.service.pointcore.entity.PointTransactionType;
import com.rewardpoint.service.pointuse.entity.PointUse;
import com.rewardpoint.service.pointuse.entity.PointUseAllocation;
import com.rewardpoint.service.pointcore.exception.PointOperationException;
import com.rewardpoint.service.pointearn.repository.PointGrantRepository;
import com.rewardpoint.service.pointcore.repository.PointTransactionRepository;
import com.rewardpoint.service.pointuse.repository.PointUseAllocationRepository;
import com.rewardpoint.service.pointuse.repository.PointUseRepository;
import com.rewardpoint.service.pointcore.support.dto.PointOperationLineResult;
import com.rewardpoint.service.pointcore.support.dto.PointOperationResult;
import com.rewardpoint.service.pointearn.support.PointExpirationService;
import com.rewardpoint.service.pointcore.support.TransactionKeyGenerator;
import com.rewardpoint.service.pointuse.service.dto.UsePointCommand;
import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.exception.PointAccountNotFoundException;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointUseService {

    private final PointAccountRepository pointAccountRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointGrantRepository pointGrantRepository;
    private final PointUseRepository pointUseRepository;
    private final PointUseAllocationRepository pointUseAllocationRepository;
    private final PointExpirationService pointExpirationService;
    private final TransactionKeyGenerator transactionKeyGenerator;

    @Transactional
    public PointOperationResult use(UsePointCommand command) {
        PointAccount account = getAccount(command.accountId());

        if (command.amount() < 1) {
            throw new PointOperationException("사용 금액은 1 이상이어야 합니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        pointExpirationService.expire(account, now);

        if (account.getCurrentBalance() < command.amount()) {
            throw new PointOperationException("사용 가능한 포인트가 부족합니다.");
        }

        List<PointGrant> grants = pointGrantRepository.findAvailableGrantsForUse(account.getAccountId(), now);
        long remaining = command.amount();
        List<PointOperationLineResult> lines = new ArrayList<>();

        String transactionKey = transactionKeyGenerator.generate();
        PointTransaction transaction = new PointTransaction(
                transactionKey,
                account,
                PointTransactionType.USE,
                command.amount(),
                null,
                command.orderNo(),
                command.description()
        );

        PointTransaction savedTransaction = pointTransactionRepository.save(transaction);
        PointUse savedPointUse = pointUseRepository.save(
                new PointUse(savedTransaction, account, command.orderNo(), command.amount())
        );

        for (PointGrant grant : grants) {
            if (remaining == 0) {
                break;
            }
            long useAmount = Math.min(remaining, grant.getRemainingAmount());
            if (useAmount <= 0) {
                continue;
            }

            grant.use(useAmount);

            PointUseAllocation pointUseAllocation = new PointUseAllocation(savedPointUse, grant, useAmount);
            pointUseAllocationRepository.save(pointUseAllocation);

            PointOperationLineResult pointOperationLineResult = new PointOperationLineResult(
                    grant.getTransaction().getTransactionKey(),
                    useAmount,
                    null,
                    null
            );
            lines.add(pointOperationLineResult);
            remaining -= useAmount;
        }

        if (remaining > 0) {
            throw new PointOperationException("요청한 금액만큼 사용할 수 있는 지급 건이 부족합니다.");
        }

        account.use(command.amount());

        return new PointOperationResult(
                account.getAccountId(),
                savedTransaction.getTransactionKey(),
                savedTransaction.getTransactionType(),
                command.amount(),
                account.getCurrentBalance(),
                command.orderNo(),
                lines
        );
    }

    private PointAccount getAccount(Long accountId) {
        return pointAccountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new PointAccountNotFoundException(accountId));
    }
}
