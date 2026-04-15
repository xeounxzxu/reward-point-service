package com.rewardpoint.service.pointledger.repository;

import com.rewardpoint.service.pointledger.entity.PointTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    Optional<PointTransaction> findByTransactionKey(String transactionKey);
}
