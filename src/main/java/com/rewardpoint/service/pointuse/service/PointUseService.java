package com.rewardpoint.service.pointuse.service;

import com.rewardpoint.service.pointledger.entity.PointGrant;
import com.rewardpoint.service.pointledger.entity.PointGrantType;
import com.rewardpoint.service.pointledger.entity.PointPolicy;
import com.rewardpoint.service.pointledger.entity.PointTransaction;
import com.rewardpoint.service.pointledger.entity.PointTransactionType;
import com.rewardpoint.service.pointledger.entity.PointUse;
import com.rewardpoint.service.pointledger.entity.PointUseAllocation;
import com.rewardpoint.service.pointledger.entity.PointUseCancel;
import com.rewardpoint.service.pointledger.entity.PointUseCancelAllocation;
import com.rewardpoint.service.pointledger.entity.PointUseCancelRestoreType;
import com.rewardpoint.service.pointledger.exception.PointOperationException;
import com.rewardpoint.service.pointledger.exception.PointTransactionNotFoundException;
import com.rewardpoint.service.pointledger.repository.PointGrantRepository;
import com.rewardpoint.service.pointledger.repository.PointTransactionRepository;
import com.rewardpoint.service.pointledger.repository.PointUseAllocationRepository;
import com.rewardpoint.service.pointledger.repository.PointUseCancelAllocationRepository;
import com.rewardpoint.service.pointledger.repository.PointUseCancelRepository;
import com.rewardpoint.service.pointledger.repository.PointUseRepository;
import com.rewardpoint.service.pointledger.support.dto.PointOperationLineResult;
import com.rewardpoint.service.pointledger.support.dto.PointOperationResult;
import com.rewardpoint.service.pointledger.support.PointExpirationService;
import com.rewardpoint.service.pointledger.support.PointPolicyService;
import com.rewardpoint.service.pointledger.support.TransactionKeyGenerator;
import com.rewardpoint.service.pointuse.service.dto.CancelUsePointCommand;
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
    private final PointUseCancelRepository pointUseCancelRepository;
    private final PointUseCancelAllocationRepository pointUseCancelAllocationRepository;
    private final PointPolicyService pointPolicyService;
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

        PointUse pointUse = new PointUse(savedTransaction, account, command.orderNo(), command.amount());
        PointUse savedPointUse = pointUseRepository.save(pointUse);

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

        PointOperationResult pointOperationResult = new PointOperationResult(
                account.getAccountId(),
                savedTransaction.getTransactionKey(),
                savedTransaction.getTransactionType(),
                command.amount(),
                account.getCurrentBalance(),
                command.orderNo(),
                lines
        );

        return pointOperationResult;
    }

    @Transactional
    public PointOperationResult cancelUse(CancelUsePointCommand command) {
        PointAccount account = getAccount(command.accountId());

        if (command.cancelAmount() < 1) {
            throw new PointOperationException("사용취소 금액은 1 이상이어야 합니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        pointExpirationService.expire(account, now);

        PointTransaction targetTransaction = getTransaction(command.targetTransactionKey());
        PointUse targetUse = pointUseRepository.findByTransaction(targetTransaction)
                .orElseThrow(() -> new PointOperationException("취소 대상 사용 이력을 찾을 수 없습니다."));

        if (!targetUse.getAccount().getAccountId().equals(account.getAccountId())) {
            throw new PointOperationException("취소 대상 사용 이력이 해당 계정에 속하지 않습니다.");
        }
        if (targetUse.getRemainingCancelableAmount() < command.cancelAmount()) {
            throw new PointOperationException("사용취소 금액이 남은 취소 가능 금액을 초과할 수 없습니다.");
        }

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

        PointUseCancel pointUseCancel = new PointUseCancel(savedCancelTransaction, targetUse, command.cancelAmount());
        PointUseCancel savedPointUseCancel = pointUseCancelRepository.save(pointUseCancel);

        long remaining = command.cancelAmount();
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
                PointGrant savedReissuedGrant = pointGrantRepository.save(reissuedGrant);

                PointUseCancelAllocation pointUseCancelAllocation = new PointUseCancelAllocation(
                        savedPointUseCancel,
                        allocation,
                        restoreAmount,
                        PointUseCancelRestoreType.REISSUE_NEW_LOT,
                        savedReissuedGrant
                );
                pointUseCancelAllocationRepository.save(pointUseCancelAllocation);

                PointOperationLineResult pointOperationLineResult = new PointOperationLineResult(
                        originalGrant.getTransaction().getTransactionKey(),
                        restoreAmount,
                        PointUseCancelRestoreType.REISSUE_NEW_LOT.name(),
                        savedReissuedGrant.getTransaction().getTransactionKey()
                );
                lines.add(pointOperationLineResult);
            } else {
                originalGrant.restore(restoreAmount);

                PointUseCancelAllocation pointUseCancelAllocation = new PointUseCancelAllocation(
                        savedPointUseCancel,
                        allocation,
                        restoreAmount,
                        PointUseCancelRestoreType.RESTORE_ORIGINAL_LOT,
                        null
                );
                pointUseCancelAllocationRepository.save(pointUseCancelAllocation);

                PointOperationLineResult pointOperationLineResult = new PointOperationLineResult(
                        originalGrant.getTransaction().getTransactionKey(),
                        restoreAmount,
                        PointUseCancelRestoreType.RESTORE_ORIGINAL_LOT.name(),
                        null
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

        PointOperationResult pointOperationResult = new PointOperationResult(
                account.getAccountId(),
                savedCancelTransaction.getTransactionKey(),
                savedCancelTransaction.getTransactionType(),
                command.cancelAmount(),
                account.getCurrentBalance(),
                targetUse.getOrderNo(),
                lines
        );

        return pointOperationResult;
    }

    private PointAccount getAccount(Long accountId) {
        return pointAccountRepository.findById(accountId)
                .orElseThrow(() -> new PointAccountNotFoundException(accountId));
    }

    private PointTransaction getTransaction(String transactionKey) {
        return pointTransactionRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new PointTransactionNotFoundException(transactionKey));
    }
}
