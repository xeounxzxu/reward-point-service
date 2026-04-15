package com.rewardpoint.service.pointaccount.exception;

public class PointAccountNotFoundException extends RuntimeException {

    public PointAccountNotFoundException(Long accountId) {
        super("포인트 계정을 찾을 수 없습니다. accountId=" + accountId);
    }
}
