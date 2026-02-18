package org.link.linkvault.repository;

import org.link.linkvault.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @EntityGraph(attributePaths = {"user"})
    @Query(value = "SELECT a FROM AuditLog a ORDER BY a.timestamp DESC",
           countQuery = "SELECT COUNT(a) FROM AuditLog a")
    Page<AuditLog> findAllWithUser(Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.user.id = :userId ORDER BY a.timestamp DESC")
    Page<AuditLog> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.action = :action ORDER BY a.timestamp DESC")
    Page<AuditLog> findByAction(@Param("action") String action, Pageable pageable);

    @Modifying
    @Query("UPDATE AuditLog a SET a.actorUsername = (SELECT u.username FROM User u WHERE u.id = :userId), a.user = null WHERE a.user.id = :userId")
    void nullifyUserReferences(@Param("userId") Long userId);
}
