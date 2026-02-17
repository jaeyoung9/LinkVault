package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.Report;
import org.link.linkvault.entity.ReportStatus;
import org.link.linkvault.entity.ReportTargetType;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportResponseDto {

    private Long id;
    private String reporterUsername;
    private ReportTargetType targetType;
    private Long targetId;
    private String reason;
    private ReportStatus status;
    private String reviewedByUsername;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    public static ReportResponseDto from(Report report) {
        return ReportResponseDto.builder()
                .id(report.getId())
                .reporterUsername(report.getReporter() != null ? report.getReporter().getUsername() : null)
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .status(report.getStatus())
                .reviewedByUsername(report.getReviewedBy() != null ? report.getReviewedBy().getUsername() : null)
                .reviewedAt(report.getReviewedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
