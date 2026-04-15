package com.rewardpoint.service.pointcore.support;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TransactionKeyGenerator {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
