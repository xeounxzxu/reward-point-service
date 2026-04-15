package com.rewardpoint.service.pointledger.exception;

public class PointTransactionNotFoundException extends RuntimeException {

    public PointTransactionNotFoundException(String transactionKey) {
        super("포인트 거래를 찾을 수 없습니다. transactionKey=" + transactionKey);
    }
}
