package com.rewardpoint.service.pointaccount.exception;

public class PointAccountAlreadyExistsException extends RuntimeException {

    public PointAccountAlreadyExistsException(String userId) {
        super("포인트 계정이 이미 존재합니다. userId=" + userId);
    }
}
