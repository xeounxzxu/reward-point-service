package com.rewardpoint.service.pointaccount.repository;

import com.rewardpoint.service.pointaccount.entity.PointAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    Optional<PointAccount> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
