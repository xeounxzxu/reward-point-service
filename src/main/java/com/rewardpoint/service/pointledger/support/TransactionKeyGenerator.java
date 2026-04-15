package com.rewardpoint.service.pointledger.support;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TransactionKeyGenerator {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
