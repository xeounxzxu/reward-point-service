package com.rewardpoint.service.pointearn.service;

import com.rewardpoint.service.pointearn.entity.PointGrant;
import com.rewardpoint.service.pointearn.entity.PointGrantCancel;
import com.rewardpoint.service.pointearn.entity.PointGrantType;
import com.rewardpoint.service.pointcore.entity.PointPolicy;
import com.rewardpoint.service.pointcore.entity.PointTransaction;
import com.rewardpoint.service.pointcore.entity.PointTransactionType;
import com.rewardpoint.service.pointcore.exception.PointOperationException;
import com.rewardpoint.service.pointcore.exception.PointTransactionNotFoundException;
import com.rewardpoint.service.pointearn.repository.PointGrantCancelRepository;
import com.rewardpoint.service.pointearn.repository.PointGrantRepository;
import com.rewardpoint.service.pointcore.repository.PointTransactionRepository;
import com.rewardpoint.service.pointearn.service.dto.CancelEarnPointCommand;
import com.rewardpoint.service.pointearn.service.dto.EarnPointCommand;
import com.rewardpoint.service.pointcore.support.dto.PointOperationLineResult;
import com.rewardpoint.service.pointcore.support.dto.PointOperationResult;
import com.rewardpoint.service.pointearn.support.PointExpirationService;
import com.rewardpoint.service.pointcore.support.PointPolicyService;
import com.rewardpoint.service.pointcore.support.TransactionKeyGenerator;
import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.exception.PointAccountNotFoundException;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointEarnService {

    private final PointAccountRepository pointAccountRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointGrantRepository pointGrantRepository;
    private final PointGrantCancelRepository pointGrantCancelRepository;
    private final PointPolicyService pointPolicyService;
    private final PointExpirationService pointExpirationService;
    private final TransactionKeyGenerator transactionKeyGenerator;

    @Transactional
    public PointOperationResult earn(EarnPointCommand command) {
        PointAccount account = getAccount(command.accountId());
        LocalDateTime now = LocalDateTime.now();
        pointExpirationService.expire(account, now);

        PointPolicy policy = pointPolicyService.getActivePolicy();
        policy.validateEarnAmount(command.amount());
        policy.validateMaxBalance(account.getCurrentBalance(), command.amount());
        int expireDays = policy.resolveExpireDays(command.expireDays());

        String transactionKey = transactionKeyGenerator.generate();
        PointTransaction transaction = new PointTransaction(
                transactionKey,
                account,
                PointTransactionType.EARN,
                command.amount(),
                null,
                null,
                command.description()
        );

        PointTransaction savedTransaction = pointTransactionRepository.save(transaction);

        PointGrantType pointGrantType = command.manual() ? PointGrantType.MANUAL : PointGrantType.NORMAL;
        PointGrant grant = new PointGrant(
                savedTransaction,
                account,
                pointGrantType,
                command.amount(),
                now.plusDays(expireDays),
                command.manual()
        );

        PointGrant savedGrant = pointGrantRepository.save(grant);

        account.charge(command.amount());

        PointOperationLineResult pointOperationLineResult = new PointOperationLineResult(
                savedGrant.getTransaction().getTransactionKey(),
                command.amount(),
                null,
                null
        );

        PointOperationResult pointOperationResult = new PointOperationResult(
                account.getAccountId(),
                savedTransaction.getTransactionKey(),
                savedTransaction.getTransactionType(),
                command.amount(),
                account.getCurrentBalance(),
                null,
                List.of(pointOperationLineResult)
        );

        return pointOperationResult;
    }

    @Transactional
    public PointOperationResult cancelEarn(CancelEarnPointCommand command) {
        PointAccount account = getAccount(command.accountId());
        LocalDateTime now = LocalDateTime.now();
        pointExpirationService.expire(account, now);

        PointTransaction targetTransaction = getTransaction(command.targetTransactionKey());
        PointGrant targetGrant = pointGrantRepository.findByTransaction(targetTransaction)
                .orElseThrow(() -> new PointOperationException("취소 대상 지급 건을 찾을 수 없습니다."));

        if (!targetGrant.getAccount().getAccountId().equals(account.getAccountId())) {
            throw new PointOperationException("취소 대상 지급 건이 해당 계정에 속하지 않습니다.");
        }
        if (!targetGrant.isFullyUnused()) {
            throw new PointOperationException("이미 사용된 적립 포인트는 취소할 수 없습니다.");
        }

        long cancelAmount = targetGrant.getRemainingAmount();
        String transactionKey = transactionKeyGenerator.generate();
        PointTransaction transaction = new PointTransaction(
                transactionKey,
                account,
                PointTransactionType.EARN_CANCEL,
                cancelAmount,
                targetTransaction.getTransactionId(),
                null,
                command.description()
        );

        PointTransaction savedTransaction = pointTransactionRepository.save(transaction);

        PointGrantCancel pointGrantCancel = new PointGrantCancel(savedTransaction, targetGrant, cancelAmount);

        pointGrantCancelRepository.save(pointGrantCancel);
        targetGrant.cancelAll();
        account.use(cancelAmount);

        PointOperationLineResult pointOperationLineResult = new PointOperationLineResult(
                targetTransaction.getTransactionKey(),
                cancelAmount,
                null,
                null
        );

        PointOperationResult pointOperationResult = new PointOperationResult(
                account.getAccountId(),
                savedTransaction.getTransactionKey(),
                savedTransaction.getTransactionType(),
                cancelAmount,
                account.getCurrentBalance(),
                null,
                List.of(pointOperationLineResult)
        );

        return pointOperationResult;
    }

    private PointAccount getAccount(Long accountId) {
        return pointAccountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new PointAccountNotFoundException(accountId));
    }

    private PointTransaction getTransaction(String transactionKey) {
        return pointTransactionRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new PointTransactionNotFoundException(transactionKey));
    }
}
