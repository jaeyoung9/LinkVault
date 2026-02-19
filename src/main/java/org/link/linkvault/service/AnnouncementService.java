package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.AnnouncementRequestDto;
import org.link.linkvault.dto.AnnouncementResponseDto;
import org.link.linkvault.entity.*;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.*;
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
    private final AnnouncementVoteRepository announcementVoteRepository;
    private final AnnouncementPollOptionRepository announcementPollOptionRepository;
    private final AnnouncementPollVoteRepository announcementPollVoteRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public List<AnnouncementResponseDto> findVisibleForGuest() {
        processScheduledAndExpired();
        List<Announcement> announcements = announcementRepository.findPublishedForGuest();
        return announcements.stream()
                .map(a -> AnnouncementResponseDto.from(a, false, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public AnnouncementResponseDto findByIdForGuest(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + id));
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Announcement not found: " + id);
        }
        if (announcement.getTargetRole() != null) {
            throw new ResourceNotFoundException("Announcement not found: " + id);
        }
        LocalDateTime now = LocalDateTime.now();
        if (announcement.getStartAt() != null && announcement.getStartAt().isAfter(now)) {
            throw new ResourceNotFoundException("Announcement not found: " + id);
        }
        if (announcement.getEndAt() != null && announcement.getEndAt().isBefore(now)) {
            throw new ResourceNotFoundException("Announcement not found: " + id);
        }

        List<AnnouncementPollOption> pollOptions = announcementPollOptionRepository
                .findByAnnouncementIdOrderByDisplayOrderAsc(id);

        return AnnouncementResponseDto.from(announcement, false, false, null, pollOptions, null);
    }

    @Transactional
    public List<AnnouncementResponseDto> findVisibleForUser(User user) {
        processScheduledAndExpired();
        boolean isAdmin = user.getRole() == Role.SUPER_ADMIN
                || user.getRole() == Role.COMMUNITY_ADMIN
                || user.getRole() == Role.MODERATOR;
        List<Announcement> announcements = isAdmin
                ? announcementRepository.findAllPublished()
                : announcementRepository.findPublishedForRole(user.getRole());
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
        boolean isAdmin = user.getRole() == Role.SUPER_ADMIN
                || user.getRole() == Role.COMMUNITY_ADMIN
                || user.getRole() == Role.MODERATOR;
        if (!isAdmin && announcement.getTargetRole() != null && announcement.getTargetRole() != user.getRole()) {
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

        // Get user's announcement vote
        VoteType userVote = announcementVoteRepository.findByUserIdAndAnnouncementId(user.getId(), id)
                .map(AnnouncementVote::getVoteType).orElse(null);

        // Get poll options and user's poll vote
        List<AnnouncementPollOption> pollOptions = announcementPollOptionRepository
                .findByAnnouncementIdOrderByDisplayOrderAsc(id);
        Long userPollVoteOptionId = announcementPollVoteRepository
                .findByUserIdAndAnnouncementId(user.getId(), id)
                .map(pv -> pv.getPollOption().getId()).orElse(null);

        return AnnouncementResponseDto.from(announcement, isRead, isAck, userVote, pollOptions, userPollVoteOptionId);
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
                .enableComments(dto.isEnableComments())
                .enableVoting(dto.isEnableVoting())
                .createdBy(creator)
                .build();
        announcement = announcementRepository.save(announcement);

        // Save poll options
        if (dto.getPollOptions() != null && !dto.getPollOptions().isEmpty()) {
            for (int i = 0; i < dto.getPollOptions().size(); i++) {
                String text = dto.getPollOptions().get(i);
                if (text != null && !text.trim().isEmpty()) {
                    announcementPollOptionRepository.save(
                            new AnnouncementPollOption(announcement, text.trim(), i));
                }
            }
        }

        AnnouncementResponseDto result = AnnouncementResponseDto.from(announcement);
        auditLogService.log(actorUsername, AuditActionCodes.ANNOUNCEMENT_CREATE, "Announcement", result.getId(),
                AuditDetailFormatter.format("priority", String.valueOf(dto.getPriority())));
        return result;
    }

    @Transactional
    public AnnouncementResponseDto update(Long id, AnnouncementRequestDto dto, String actorUsername) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + id));
        announcement.update(dto.getTitle(), dto.getContent(), dto.getPriority(),
                dto.getTargetRole(), dto.getStartAt(), dto.getEndAt(), dto.isPinned(),
                dto.isEnableComments(), dto.isEnableVoting());

        // Update poll options only if no votes have been cast yet
        if (dto.getPollOptions() != null) {
            boolean hasVotes = announcementPollVoteRepository.existsByAnnouncementId(id);
            if (!hasVotes) {
                announcementPollOptionRepository.deleteByAnnouncementId(id);
                announcementPollOptionRepository.flush();
                for (int i = 0; i < dto.getPollOptions().size(); i++) {
                    String text = dto.getPollOptions().get(i);
                    if (text != null && !text.trim().isEmpty()) {
                        announcementPollOptionRepository.save(
                                new AnnouncementPollOption(announcement, text.trim(), i));
                    }
                }
            }
        }

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
        announcementPollVoteRepository.deleteByAnnouncementId(id);
        announcementPollOptionRepository.deleteByAnnouncementId(id);
        announcementVoteRepository.deleteByAnnouncementId(id);
        announcementReadRepository.deleteByAnnouncementId(id);
        announcementRepository.deleteById(id);
        auditLogService.log(actorUsername, AuditActionCodes.ANNOUNCEMENT_DELETE, "Announcement", id, null);
    }

    public AnnouncementResponseDto findByIdAdmin(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + id));

        List<AnnouncementPollOption> pollOptions = announcementPollOptionRepository
                .findByAnnouncementIdOrderByDisplayOrderAsc(id);

        return AnnouncementResponseDto.from(announcement, false, false, null, pollOptions, null);
    }

    public Page<AnnouncementResponseDto> findAllAdmin(Pageable pageable) {
        return announcementRepository.findAllWithCreator(pageable)
                .map(AnnouncementResponseDto::from);
    }

    @Transactional
    public AnnouncementResponseDto vote(Long announcementId, VoteType voteType, User user) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + announcementId));

        if (!announcement.isEnableVoting()) {
            throw new IllegalStateException("Voting is not enabled for this announcement");
        }

        Optional<AnnouncementVote> existingVote = announcementVoteRepository
                .findByUserIdAndAnnouncementId(user.getId(), announcementId);

        VoteType resultVote = null;

        if (existingVote.isPresent()) {
            AnnouncementVote vote = existingVote.get();
            if (vote.getVoteType() == voteType) {
                // Same vote: remove it
                if (voteType == VoteType.LIKE) announcement.decrementLikeCount();
                else announcement.decrementDislikeCount();
                announcementVoteRepository.delete(vote);
            } else {
                // Different vote: switch
                if (vote.getVoteType() == VoteType.LIKE) {
                    announcement.decrementLikeCount();
                    announcement.incrementDislikeCount();
                } else {
                    announcement.decrementDislikeCount();
                    announcement.incrementLikeCount();
                }
                vote.changeVoteType(voteType);
                resultVote = voteType;
            }
        } else {
            // New vote
            if (voteType == VoteType.LIKE) announcement.incrementLikeCount();
            else announcement.incrementDislikeCount();
            announcementVoteRepository.save(new AnnouncementVote(user, announcement, voteType));
            resultVote = voteType;
        }

        return AnnouncementResponseDto.builder()
                .likeCount(announcement.getLikeCount())
                .dislikeCount(announcement.getDislikeCount())
                .score(announcement.getLikeCount() - announcement.getDislikeCount())
                .userVote(resultVote)
                .build();
    }

    @Transactional
    public AnnouncementResponseDto pollVote(Long announcementId, Long optionId, User user) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + announcementId));

        if (!announcement.isEnableVoting()) {
            throw new IllegalStateException("Voting is not enabled for this announcement");
        }

        AnnouncementPollOption newOption = announcementPollOptionRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll option not found: " + optionId));

        if (!newOption.getAnnouncement().getId().equals(announcementId)) {
            throw new IllegalArgumentException("Poll option does not belong to this announcement");
        }

        Optional<AnnouncementPollVote> existingVote = announcementPollVoteRepository
                .findByUserIdAndAnnouncementId(user.getId(), announcementId);

        if (existingVote.isPresent()) {
            AnnouncementPollVote pv = existingVote.get();
            if (pv.getPollOption().getId().equals(optionId)) {
                // Same option: remove vote
                newOption.decrementVoteCount();
                announcementPollVoteRepository.delete(pv);
            } else {
                // Different option: switch
                pv.getPollOption().decrementVoteCount();
                newOption.incrementVoteCount();
                pv.changePollOption(newOption);
            }
        } else {
            // New poll vote
            newOption.incrementVoteCount();
            announcementPollVoteRepository.save(new AnnouncementPollVote(user, announcement, newOption));
        }

        // Return updated poll data
        List<AnnouncementPollOption> pollOptions = announcementPollOptionRepository
                .findByAnnouncementIdOrderByDisplayOrderAsc(announcementId);
        Long userPollVoteOptionId = announcementPollVoteRepository
                .findByUserIdAndAnnouncementId(user.getId(), announcementId)
                .map(pv -> pv.getPollOption().getId()).orElse(null);

        return AnnouncementResponseDto.from(announcement, false, false, null, pollOptions, userPollVoteOptionId);
    }
}
