package org.link.linkvault.repository;

import org.link.linkvault.entity.Announcement;
import org.link.linkvault.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.createdBy " +
           "WHERE a.status = 'PUBLISHED' AND (a.targetRole IS NULL OR a.targetRole = :role) " +
           "ORDER BY a.pinned DESC, a.createdAt DESC")
    List<Announcement> findPublishedForRole(@Param("role") Role role);

    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.createdBy " +
           "WHERE a.status = 'PUBLISHED' AND a.targetRole IS NULL " +
           "ORDER BY a.pinned DESC, a.createdAt DESC")
    List<Announcement> findPublishedForGuest();

    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.createdBy " +
           "WHERE a.status = 'PUBLISHED' " +
           "ORDER BY a.pinned DESC, a.createdAt DESC")
    List<Announcement> findAllPublished();

    @Query("SELECT a FROM Announcement a WHERE a.status = 'SCHEDULED' AND a.startAt <= :now")
    List<Announcement> findScheduledReadyToPublish(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM Announcement a WHERE a.status = 'PUBLISHED' AND a.endAt IS NOT NULL AND a.endAt <= :now")
    List<Announcement> findPublishedReadyToArchive(@Param("now") LocalDateTime now);

    @Query(value = "SELECT a FROM Announcement a LEFT JOIN FETCH a.createdBy ORDER BY a.createdAt DESC",
           countQuery = "SELECT COUNT(a) FROM Announcement a")
    Page<Announcement> findAllWithCreator(Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE a.status = 'PUBLISHED' " +
           "AND a.priority IN ('WARN', 'CRITICAL') AND (a.targetRole IS NULL OR a.targetRole = :role)")
    List<Announcement> findActiveHighPriorityForRole(@Param("role") Role role);

    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.status = 'PUBLISHED' " +
           "AND (a.targetRole IS NULL OR a.targetRole = :role) " +
           "AND a.id NOT IN (SELECT ar.announcement.id FROM AnnouncementRead ar WHERE ar.user.id = :userId)")
    long countUnreadForUser(@Param("userId") Long userId, @Param("role") Role role);

    List<Announcement> findByCreatedById(Long userId);
}
