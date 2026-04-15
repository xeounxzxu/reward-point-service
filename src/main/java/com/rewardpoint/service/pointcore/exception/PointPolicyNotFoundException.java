package com.rewardpoint.service.pointcore.exception;

public class PointPolicyNotFoundException extends RuntimeException {

    public PointPolicyNotFoundException() {
        super("활성화된 포인트 정책을 찾을 수 없습니다.");
    }
}
