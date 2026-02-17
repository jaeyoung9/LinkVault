package org.link.linkvault.repository;

import org.link.linkvault.entity.Report;
import org.link.linkvault.entity.ReportStatus;
import org.link.linkvault.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = 'ACTIONED' AND (" +
            "(r.targetType = 'POST' AND r.targetId IN (SELECT b.id FROM Bookmark b WHERE b.user.id = :userId)) " +
            "OR " +
            "(r.targetType = 'COMMENT' AND r.targetId IN (SELECT c.id FROM Comment c WHERE c.user.id = :userId))" +
            ")")
    long countActionedReportsByTargetUserId(@Param("userId") Long userId);
}
