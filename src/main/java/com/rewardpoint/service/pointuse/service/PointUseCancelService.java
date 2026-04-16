package com.rewardpoint.service.pointuse.service;

import com.rewardpoint.service.pointaccount.entity.PointAccount;
import com.rewardpoint.service.pointaccount.exception.PointAccountNotFoundException;
import com.rewardpoint.service.pointaccount.repository.PointAccountRepository;
import com.rewardpoint.service.pointearn.entity.PointGrant;
import com.rewardpoint.service.pointearn.entity.PointGrantType;
import com.rewardpoint.service.pointearn.repository.PointGrantRepository;
import com.rewardpoint.service.pointearn.support.PointExpirationService;
import com.rewardpoint.service.pointcore.entity.PointPolicy;
import com.rewardpoint.service.pointcore.entity.PointTransaction;
import com.rewardpoint.service.pointcore.entity.PointTransactionType;
import com.rewardpoint.service.pointcore.exception.PointOperationException;
import com.rewardpoint.service.pointcore.exception.PointTransactionNotFoundException;
import com.rewardpoint.service.pointcore.repository.PointTransactionRepository;
import com.rewardpoint.service.pointcore.support.PointPolicyService;
import com.rewardpoint.service.pointcore.support.TransactionKeyGenerator;
import com.rewardpoint.service.pointcore.support.dto.PointOperationLineResult;
import com.rewardpoint.service.pointcore.support.dto.PointOperationResult;
import com.rewardpoint.service.pointuse.entity.PointUse;
import com.rewardpoint.service.pointuse.entity.PointUseAllocation;
import com.rewardpoint.service.pointuse.entity.PointUseCancel;
import com.rewardpoint.service.pointuse.entity.PointUseCancelAllocation;
import com.rewardpoint.service.pointuse.entity.PointUseCancelRestoreType;
import com.rewardpoint.service.pointuse.repository.PointUseAllocationRepository;
import com.rewardpoint.service.pointuse.repository.PointUseCancelAllocationRepository;
import com.rewardpoint.service.pointuse.repository.PointUseCancelRepository;
import com.rewardpoint.service.pointuse.repository.PointUseRepository;
import com.rewardpoint.service.pointuse.service.dto.CancelUsePointCommand;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointUseCancelService {

    private final PointAccountRepository pointAccountRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointGrantRepository pointGrantRepository;
    private final PointUseRepository pointUseRepository;
    private final PointUseAllocationRepository pointUseAllocationRepository;
    private final PointUseCancelRepository pointUseCancelRepository;
    private final PointUseCancelAllocationRepository pointUseCancelAllocationRepository;
    private final PointPolicyService pointPolicyService;
    private final PointExpirationService pointExpirationService;
    private final TransactionKeyGenerator transactionKeyGenerator;

    @Transactional
    public PointOperationResult cancelUse(CancelUsePointCommand command) {
        PointAccount account = getAccount(command.accountId());
        LocalDateTime now = LocalDateTime.now();
        validateCancelUseCommand(command);
        pointExpirationService.expire(account, now);
        PointTransaction targetTransaction = getTransaction(command.targetTransactionKey());
        PointUse targetUse = pointUseRepository.findByTransaction(targetTransaction)
                .orElseThrow(() -> new PointOperationException("취소 대상 사용 이력을 찾을 수 없습니다."));
        validateCancelableTarget(account, targetUse, command.cancelAmount());

        String transactionKey = transactionKeyGenerator.generate();
        PointTransaction cancelTransaction = new PointTransaction(
                transactionKey,
                account,
                PointTransactionType.USE_CANCEL,
                command.cancelAmount(),
                targetTransaction.getTransactionId(),
                targetUse.getOrderNo(),
                command.description()
        );
        PointTransaction savedCancelTransaction = pointTransactionRepository.save(cancelTransaction);
        PointUseCancel savedPointUseCancel = pointUseCancelRepository.save(
                new PointUseCancel(savedCancelTransaction, targetUse, command.cancelAmount())
        );
        List<PointOperationLineResult> lines = restoreAllocations(
                account,
                targetUse,
                savedPointUseCancel,
                savedCancelTransaction,
                command.cancelAmount(),
                now
        );

        return new PointOperationResult(
                account.getAccountId(),
                savedCancelTransaction.getTransactionKey(),
                savedCancelTransaction.getTransactionType(),
                command.cancelAmount(),
                account.getCurrentBalance(),
                targetUse.getOrderNo(),
                lines
        );
    }

    private void validateCancelUseCommand(CancelUsePointCommand command) {
        if (command.cancelAmount() < 1) {
            throw new PointOperationException("사용취소 금액은 1 이상이어야 합니다.");
        }
    }

    private void validateCancelableTarget(PointAccount account, PointUse targetUse, long cancelAmount) {
        if (!targetUse.getAccount().getAccountId().equals(account.getAccountId())) {
            throw new PointOperationException("취소 대상 사용 이력이 해당 계정에 속하지 않습니다.");
        }
        if (targetUse.getRemainingCancelableAmount() < cancelAmount) {
            throw new PointOperationException("사용취소 금액이 남은 취소 가능 금액을 초과할 수 없습니다.");
        }
    }

    private List<PointOperationLineResult> restoreAllocations(
            PointAccount account,
            PointUse targetUse,
            PointUseCancel savedPointUseCancel,
            PointTransaction savedCancelTransaction,
            long cancelAmount,
            LocalDateTime now
    ) {
        long remaining = cancelAmount;
        List<PointOperationLineResult> lines = new ArrayList<>();
        List<PointUseAllocation> allocations = pointUseAllocationRepository.findByPointUseOrderByAllocationIdAsc(targetUse);

        for (PointUseAllocation allocation : allocations) {
            if (remaining == 0) {
                break;
            }
            long restoreAmount = Math.min(remaining, allocation.getRemainingCancelableAmount());
            if (restoreAmount <= 0) {
                continue;
            }

            PointGrant originalGrant = allocation.getGrant();
            allocation.cancel(restoreAmount);

            if (originalGrant.isExpired(now)) {
                PointOperationLineResult pointOperationLineResult = restoreExpiredAllocation(
                        account,
                        savedPointUseCancel,
                        savedCancelTransaction,
                        allocation,
                        originalGrant,
                        restoreAmount,
                        now
                );
                lines.add(pointOperationLineResult);
            } else {
                PointOperationLineResult pointOperationLineResult = restoreActiveAllocation(
                        savedPointUseCancel,
                        allocation,
                        originalGrant,
                        restoreAmount
                );
                lines.add(pointOperationLineResult);
            }

            targetUse.cancel(restoreAmount);
            account.charge(restoreAmount);
            remaining -= restoreAmount;
        }

        if (remaining > 0) {
            throw new PointOperationException("요청한 금액만큼 취소할 allocation 잔액이 부족합니다.");
        }

        return lines;
    }

    private PointOperationLineResult restoreExpiredAllocation(
            PointAccount account,
            PointUseCancel savedPointUseCancel,
            PointTransaction savedCancelTransaction,
            PointUseAllocation allocation,
            PointGrant originalGrant,
            long restoreAmount,
            LocalDateTime now
    ) {
        PointGrant savedReissuedGrant = reissueGrant(account, savedCancelTransaction, restoreAmount, now);

        PointUseCancelAllocation pointUseCancelAllocation = new PointUseCancelAllocation(
                savedPointUseCancel,
                allocation,
                restoreAmount,
                PointUseCancelRestoreType.REISSUE_NEW_LOT,
                savedReissuedGrant
        );
        pointUseCancelAllocationRepository.save(pointUseCancelAllocation);

        return new PointOperationLineResult(
                originalGrant.getTransaction().getTransactionKey(),
                restoreAmount,
                PointUseCancelRestoreType.REISSUE_NEW_LOT.name(),
                savedReissuedGrant.getTransaction().getTransactionKey()
        );
    }

    private PointOperationLineResult restoreActiveAllocation(
            PointUseCancel savedPointUseCancel,
            PointUseAllocation allocation,
            PointGrant originalGrant,
            long restoreAmount
    ) {
        originalGrant.restore(restoreAmount);

        PointUseCancelAllocation pointUseCancelAllocation = new PointUseCancelAllocation(
                savedPointUseCancel,
                allocation,
                restoreAmount,
                PointUseCancelRestoreType.RESTORE_ORIGINAL_LOT,
                null
        );
        pointUseCancelAllocationRepository.save(pointUseCancelAllocation);

        return new PointOperationLineResult(
                originalGrant.getTransaction().getTransactionKey(),
                restoreAmount,
                PointUseCancelRestoreType.RESTORE_ORIGINAL_LOT.name(),
                null
        );
    }

    private PointGrant reissueGrant(
            PointAccount account,
            PointTransaction savedCancelTransaction,
            long restoreAmount,
            LocalDateTime now
    ) {
        String reissueKey = transactionKeyGenerator.generate();
        PointTransaction reissueTransaction = new PointTransaction(
                reissueKey,
                account,
                PointTransactionType.REISSUE,
                restoreAmount,
                savedCancelTransaction.getTransactionId(),
                null,
                "사용취소로 인한 재적립"
        );
        PointTransaction savedReissueTransaction = pointTransactionRepository.save(reissueTransaction);

        PointPolicy pointPolicy = pointPolicyService.getActivePolicy();
        PointGrant reissuedGrant = new PointGrant(
                savedReissueTransaction,
                account,
                PointGrantType.USE_CANCEL_REISSUE,
                restoreAmount,
                now.plusDays(pointPolicy.getDefaultExpireDays()),
                false
        );

        return pointGrantRepository.save(reissuedGrant);
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
