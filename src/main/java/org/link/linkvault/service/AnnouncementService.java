package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.AnnouncementRequestDto;
import org.link.linkvault.dto.AnnouncementResponseDto;
import org.link.linkvault.entity.*;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.AnnouncementReadRepository;
import org.link.linkvault.repository.AnnouncementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public List<AnnouncementResponseDto> findVisibleForUser(User user) {
        processScheduledAndExpired();
        List<Announcement> announcements = announcementRepository.findPublishedForRole(user.getRole());
        return announcements.stream()
                .map(a -> {
                    Optional<AnnouncementRead> readOpt = announcementReadRepository
                            .findByUserIdAndAnnouncementId(user.getId(), a.getId());
                    boolean isRead = readOpt.isPresent();
                    boolean isAck = readOpt.map(ar -> ar.getAcknowledgedAt() != null).orElse(false);
                    return AnnouncementResponseDto.from(a, isRead, isAck);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public AnnouncementResponseDto findById(Long id, User user) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + id));
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Announcement not found: " + id);
        }
        if (announcement.getTargetRole() != null && announcement.getTargetRole() != user.getRole()) {
            throw new ResourceNotFoundException("Announcement not found: " + id);
        }
        LocalDateTime now = LocalDateTime.now();
        if (announcement.getStartAt() != null && announcement.getStartAt().isAfter(now)) {
            throw new ResourceNotFoundException("Announcement not found: " + id);
        }
        if (announcement.getEndAt() != null && announcement.getEndAt().isBefore(now)) {
            throw new ResourceNotFoundException("Announcement not found: " + id);
        }
        Optional<AnnouncementRead> readOpt = announcementReadRepository
                .findByUserIdAndAnnouncementId(user.getId(), id);
        boolean isRead = readOpt.isPresent();
        boolean isAck = readOpt.map(ar -> ar.getAcknowledgedAt() != null).orElse(false);
        return AnnouncementResponseDto.from(announcement, isRead, isAck);
    }

    @Transactional
    public void markAsRead(Long announcementId, User user) {
        if (!announcementReadRepository.existsByUserIdAndAnnouncementId(user.getId(), announcementId)) {
            Announcement announcement = announcementRepository.findById(announcementId)
                    .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + announcementId));
            announcementReadRepository.save(new AnnouncementRead(user, announcement));
        }
    }

    @Transactional
    public void acknowledge(Long announcementId, User user) {
        Optional<AnnouncementRead> readOpt = announcementReadRepository
                .findByUserIdAndAnnouncementId(user.getId(), announcementId);
        if (readOpt.isPresent()) {
            readOpt.get().acknowledge();
        } else {
            Announcement announcement = announcementRepository.findById(announcementId)
                    .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + announcementId));
            AnnouncementRead ar = new AnnouncementRead(user, announcement);
            ar.acknowledge();
            announcementReadRepository.save(ar);
        }
    }

    public long getUnreadCountForUser(User user) {
        return announcementRepository.countUnreadForUser(user.getId(), user.getRole());
    }

    @Transactional
    public List<AnnouncementResponseDto> findActiveHighPriority(User user) {
        processScheduledAndExpired();
        return announcementRepository.findActiveHighPriorityForRole(user.getRole()).stream()
                .map(AnnouncementResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void processScheduledAndExpired() {
        LocalDateTime now = LocalDateTime.now();
        for (Announcement a : announcementRepository.findScheduledReadyToPublish(now)) {
            a.updateStatus(AnnouncementStatus.PUBLISHED);
        }
        for (Announcement a : announcementRepository.findPublishedReadyToArchive(now)) {
            a.updateStatus(AnnouncementStatus.ARCHIVED);
        }
    }

    @Transactional
    public AnnouncementResponseDto create(AnnouncementRequestDto dto, User creator, String actorUsername) {
        Announcement announcement = Announcement.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .priority(dto.getPriority())
                .status(dto.getStatus() != null ? dto.getStatus() : AnnouncementStatus.DRAFT)
                .targetRole(dto.getTargetRole())
                .startAt(dto.getStartAt())
                .endAt(dto.getEndAt())
                .pinned(dto.isPinned())
                .createdBy(creator)
                .build();
        AnnouncementResponseDto result = AnnouncementResponseDto.from(announcementRepository.save(announcement));
        auditLogService.log(actorUsername, AuditActionCodes.ANNOUNCEMENT_CREATE, "Announcement", result.getId(),
                AuditDetailFormatter.format("priority", String.valueOf(dto.getPriority())));
        return result;
    }

    @Transactional
    public AnnouncementResponseDto update(Long id, AnnouncementRequestDto dto, String actorUsername) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + id));
        announcement.update(dto.getTitle(), dto.getContent(), dto.getPriority(),
                dto.getTargetRole(), dto.getStartAt(), dto.getEndAt(), dto.isPinned());
        auditLogService.log(actorUsername, AuditActionCodes.ANNOUNCEMENT_UPDATE, "Announcement", id, null);
        return AnnouncementResponseDto.from(announcement);
    }

    @Transactional
    public void updateStatus(Long id, AnnouncementStatus status, String actorUsername) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + id));
        announcement.updateStatus(status);
        auditLogService.log(actorUsername, AuditActionCodes.ANNOUNCEMENT_STATUS, "Announcement", id,
                AuditDetailFormatter.format("status", String.valueOf(status)));
    }

    @Transactional
    public void delete(Long id, String actorUsername) {
        if (!announcementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Announcement not found: " + id);
        }
        announcementReadRepository.deleteByAnnouncementId(id);
        announcementRepository.deleteById(id);
        auditLogService.log(actorUsername, AuditActionCodes.ANNOUNCEMENT_DELETE, "Announcement", id, null);
    }

    public AnnouncementResponseDto findByIdAdmin(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + id));
        return AnnouncementResponseDto.from(announcement);
    }

    public Page<AnnouncementResponseDto> findAllAdmin(Pageable pageable) {
        return announcementRepository.findAllWithCreator(pageable)
                .map(AnnouncementResponseDto::from);
    }
}
