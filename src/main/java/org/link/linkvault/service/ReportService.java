package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.dto.ReportRequestDto;
import org.link.linkvault.dto.ReportResponseDto;
import org.link.linkvault.entity.*;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.BookmarkRepository;
import org.link.linkvault.repository.CommentRepository;
import org.link.linkvault.repository.ReportRepository;
import org.link.linkvault.repository.UserRepository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final SystemSettingsService systemSettingsService;
    private final AuditLogService auditLogService;

    private static final int DEFAULT_AUTO_DISABLE_THRESHOLD = 5;

    @Transactional
    public ReportResponseDto submit(User reporter, ReportRequestDto dto) {
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporter.getId(), dto.getTargetType(), dto.getTargetId())) {
            throw new IllegalStateException("You have already reported this content");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(dto.getTargetType())
                .targetId(dto.getTargetId())
                .reason(dto.getReason())
                .build();

        return ReportResponseDto.from(reportRepository.save(report));
    }

    public Page<ReportResponseDto> findAll(Pageable pageable) {
        return reportRepository.findAll(pageable).map(ReportResponseDto::from);
    }

    public Page<ReportResponseDto> findByStatus(ReportStatus status, Pageable pageable) {
        return reportRepository.findByStatus(status, pageable).map(ReportResponseDto::from);
    }

    @Transactional
    public ReportResponseDto review(Long id, User reviewer, ReportStatus status) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
        report.review(reviewer, status);

        if (status == ReportStatus.ACTIONED) {
            checkAndDisableUser(report, reviewer);
        }

        return ReportResponseDto.from(report);
    }

    public long getPendingCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    private void checkAndDisableUser(Report report, User reviewer) {
        User targetUser = resolveTargetUser(report);
        if (targetUser == null || !targetUser.isEnabled()) {
            return;
        }

        // Don't auto-disable admins
        if (targetUser.getRole() == Role.SUPER_ADMIN || targetUser.getRole() == Role.COMMUNITY_ADMIN) {
            return;
        }

        int threshold = getAutoDisableThreshold();
        long actionedCount = reportRepository.countActionedReportsByTargetUserId(targetUser.getId());

        if (actionedCount >= threshold) {
            targetUser.deactivateForPrivacy("Auto-disabled: " + actionedCount + " actioned reports exceeded threshold of " + threshold);
            userRepository.save(targetUser);
            auditLogService.log(reviewer.getUsername(), "AUTO_DISABLE_USER", "User", targetUser.getId(),
                    "User '" + targetUser.getUsername() + "' auto-disabled after " + actionedCount + " actioned reports");
            log.info("User '{}' auto-disabled: {} actioned reports >= threshold {}", targetUser.getUsername(), actionedCount, threshold);

            // Bulk soft-delete all user's posts and comments
            List<Bookmark> userBookmarks = bookmarkRepository.findAllByUserId(targetUser.getId());
            int deletedPosts = 0;
            for (Bookmark bm : userBookmarks) {
                if (!bm.isDeleted()) {
                    bm.softDelete();
                    deletedPosts++;
                }
            }

            List<Comment> userComments = commentRepository.findByUserId(targetUser.getId());
            int deletedComments = 0;
            for (Comment c : userComments) {
                if (!c.isDeleted()) {
                    c.softDelete();
                    deletedComments++;
                }
            }

            auditLogService.log(reviewer.getUsername(), "BULK_SOFT_DELETE_CONTENT", "User", targetUser.getId(),
                    "Soft-deleted " + deletedPosts + " posts and " + deletedComments + " comments for disabled user '" + targetUser.getUsername() + "'");
            log.info("Bulk soft-deleted {} posts and {} comments for disabled user '{}'", deletedPosts, deletedComments, targetUser.getUsername());
        }
    }

    private User resolveTargetUser(Report report) {
        if (report.getTargetType() == ReportTargetType.POST) {
            return bookmarkRepository.findById(report.getTargetId())
                    .map(Bookmark::getUser)
                    .orElse(null);
        } else if (report.getTargetType() == ReportTargetType.COMMENT) {
            return commentRepository.findById(report.getTargetId())
                    .map(Comment::getUser)
                    .orElse(null);
        }
        return null;
    }

    private int getAutoDisableThreshold() {
        return systemSettingsService.getValue("report.auto-disable-threshold")
                .map(val -> {
                    try {
                        return Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return DEFAULT_AUTO_DISABLE_THRESHOLD;
                    }
                })
                .orElse(DEFAULT_AUTO_DISABLE_THRESHOLD);
    }
}
