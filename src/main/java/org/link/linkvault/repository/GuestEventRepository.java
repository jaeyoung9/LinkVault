package org.link.linkvault.repository;

import org.link.linkvault.entity.GuestEvent;
import org.link.linkvault.entity.GuestEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface GuestEventRepository extends JpaRepository<GuestEvent, Long> {

    @Query("SELECT COUNT(DISTINCT e.sessionId) FROM GuestEvent e WHERE e.eventType = :eventType AND e.createdAt BETWEEN :from AND :to")
    long countDistinctSessionsByEventType(@Param("eventType") GuestEventType eventType,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    long countBySessionIdAndEventType(String sessionId, GuestEventType eventType);

    long countBySessionIdAndCreatedAtAfter(String sessionId, LocalDateTime after);
}
