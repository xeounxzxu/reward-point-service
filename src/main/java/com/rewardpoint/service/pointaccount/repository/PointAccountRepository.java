package com.rewardpoint.service.pointaccount.repository;

import com.rewardpoint.service.pointaccount.entity.PointAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    Optional<PointAccount> findByUserId(String userId);

    boolean existsByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
            from PointAccount a
            where a.accountId = :accountId
            """)
    Optional<PointAccount> findByIdForUpdate(Long accountId);
}
