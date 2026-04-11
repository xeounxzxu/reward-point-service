package com.rewardpoint.service.pointaccount.exception;

public class PointAccountNotFoundException extends RuntimeException {

    public PointAccountNotFoundException(Long accountId) {
        super("Point account not found. accountId=" + accountId);
    }
}
