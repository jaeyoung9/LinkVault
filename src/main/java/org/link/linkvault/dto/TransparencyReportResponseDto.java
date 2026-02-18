package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.TransparencyReport;

import java.time.LocalDateTime;

@Getter
@Builder
public class TransparencyReportResponseDto {
    private Long id;
    private String title;
    private String period;
    private String content;
    private int totalDonationsCents;
    private int totalPassRevenueCents;
    private boolean published;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    public static TransparencyReportResponseDto from(TransparencyReport r) {
        return TransparencyReportResponseDto.builder()
                .id(r.getId())
                .title(r.getTitle())
                .period(r.getPeriod())
                .content(r.getContent())
                .totalDonationsCents(r.getTotalDonationsCents())
                .totalPassRevenueCents(r.getTotalPassRevenueCents())
                .published(r.isPublished())
                .publishedAt(r.getPublishedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
