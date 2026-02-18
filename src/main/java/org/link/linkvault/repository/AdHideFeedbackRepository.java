package org.link.linkvault.repository;

import org.link.linkvault.entity.AdHideFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdHideFeedbackRepository extends JpaRepository<AdHideFeedback, Long> {

    @Query("SELECT f FROM AdHideFeedback f WHERE f.user.id = :userId OR f.sessionId = :sessionId")
    List<AdHideFeedback> findByUserIdOrSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);

    List<AdHideFeedback> findBySessionId(String sessionId);
}
