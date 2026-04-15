package com.rewardpoint.service.pointledger.repository;

import com.rewardpoint.service.pointledger.entity.PointGrant;
import com.rewardpoint.service.pointledger.entity.PointTransaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PointGrantRepository extends JpaRepository<PointGrant, Long> {

    Optional<PointGrant> findByTransaction(PointTransaction transaction);

    @Query("""
            select l
            from PointGrant l
            where l.account.accountId = :accountId
              and l.remainingAmount > 0
              and l.expireAt > :now
            order by case when l.manual = true then 0 else 1 end, l.expireAt asc, l.grantId asc
            """)
    List<PointGrant> findAvailableGrantsForUse(Long accountId, LocalDateTime now);

    @Query("""
            select l
            from PointGrant l
            where l.account.accountId = :accountId
              and l.remainingAmount > 0
              and l.expireAt <= :now
            order by l.expireAt asc, l.grantId asc
            """)
    List<PointGrant> findExpiredGrants(Long accountId, LocalDateTime now);
}
