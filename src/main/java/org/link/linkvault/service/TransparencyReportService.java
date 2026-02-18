package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.TransparencyReportRequestDto;
import org.link.linkvault.dto.TransparencyReportResponseDto;
import org.link.linkvault.entity.TransparencyReport;
import org.link.linkvault.entity.User;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.TransparencyReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransparencyReportService {

    private final TransparencyReportRepository reportRepository;
    private final AuditLogService auditLogService;

    public List<TransparencyReportResponseDto> findPublished() {
        return reportRepository.findByPublishedTrue().stream()
                .map(TransparencyReportResponseDto::from)
                .collect(Collectors.toList());
    }

    public TransparencyReportResponseDto findById(Long id) {
        return reportRepository.findById(id)
                .map(TransparencyReportResponseDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
    }

    public Page<TransparencyReportResponseDto> findAll(Pageable pageable) {
        return reportRepository.findAllOrderByCreatedAtDesc(pageable)
                .map(TransparencyReportResponseDto::from);
    }

    @Transactional
    public TransparencyReportResponseDto create(TransparencyReportRequestDto request, User createdBy) {
        TransparencyReport report = TransparencyReport.builder()
                .title(request.getTitle())
                .period(request.getPeriod())
                .content(request.getContent())
                .totalDonationsCents(request.getTotalDonationsCents())
                .totalPassRevenueCents(request.getTotalPassRevenueCents())
                .createdBy(createdBy)
                .build();
        return TransparencyReportResponseDto.from(reportRepository.save(report));
    }

    @Transactional
    public TransparencyReportResponseDto update(Long id, TransparencyReportRequestDto request) {
        TransparencyReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
        report.update(request.getTitle(), request.getPeriod(), request.getContent(),
                request.getTotalDonationsCents(), request.getTotalPassRevenueCents());
        return TransparencyReportResponseDto.from(report);
    }

    @Transactional
    public void publish(Long id, String actorUsername) {
        TransparencyReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
        report.publish();
        auditLogService.log(actorUsername, AuditActionCodes.TRANSPARENCY_REPORT_PUBLISH,
                "TransparencyReport", id, "title=" + report.getTitle());
    }

    @Transactional
    public void unpublish(Long id) {
        TransparencyReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
        report.unpublish();
    }

    @Transactional
    public void delete(Long id) {
        reportRepository.deleteById(id);
    }
}
