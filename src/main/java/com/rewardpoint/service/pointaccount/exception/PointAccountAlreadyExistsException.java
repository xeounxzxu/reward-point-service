package com.rewardpoint.service.pointaccount.exception;

public class PointAccountAlreadyExistsException extends RuntimeException {

    public PointAccountAlreadyExistsException(String userId) {
        super("Point account already exists. userId=" + userId);
    }
}
