package org.link.linkvault.repository;

import org.link.linkvault.entity.RewardAction;
import org.link.linkvault.entity.RewardPointsLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RewardPointsLedgerRepository extends JpaRepository<RewardPointsLedger, Long> {

    long countByUserIdAndActionAndCreatedAtAfter(Long userId, RewardAction action, LocalDateTime after);

    @Query("SELECT COALESCE(SUM(r.points), 0) FROM RewardPointsLedger r WHERE r.user.id = :userId")
    int sumPointsByUserId(@Param("userId") Long userId);
}
