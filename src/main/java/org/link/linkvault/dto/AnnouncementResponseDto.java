package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.Announcement;
import org.link.linkvault.entity.AnnouncementPollOption;
import org.link.linkvault.entity.AnnouncementPriority;
import org.link.linkvault.entity.AnnouncementStatus;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.VoteType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class AnnouncementResponseDto {
    private Long id;
    private String title;
    private String content;
    private AnnouncementPriority priority;
    private AnnouncementStatus status;
    private Role targetRole;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean pinned;
    private boolean enableComments;
    private boolean enableVoting;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean read;
    private boolean acknowledged;

    // Voting fields
    private int likeCount;
    private int dislikeCount;
    private int score;
    private VoteType userVote;

    // Poll fields
    private List<PollOptionDto> pollOptions;
    private Long userPollVoteOptionId;

    @Getter
    @Builder
    public static class PollOptionDto {
        private Long id;
        private String text;
        private int voteCount;
        private double percentage;
        private boolean selected;
    }

    public static AnnouncementResponseDto from(Announcement a) {
        return from(a, false, false);
    }

    public static AnnouncementResponseDto from(Announcement a, boolean isRead, boolean isAcknowledged) {
        return from(a, isRead, isAcknowledged, null, null, null);
    }

    public static AnnouncementResponseDto from(Announcement a, boolean isRead, boolean isAcknowledged,
                                                 VoteType userVote,
                                                 List<AnnouncementPollOption> pollOptionEntities,
                                                 Long userPollVoteOptionId) {
        List<PollOptionDto> pollDtos = Collections.emptyList();
        if (pollOptionEntities != null && !pollOptionEntities.isEmpty()) {
            int totalVotes = pollOptionEntities.stream().mapToInt(AnnouncementPollOption::getVoteCount).sum();
            pollDtos = pollOptionEntities.stream()
                    .map(opt -> PollOptionDto.builder()
                            .id(opt.getId())
                            .text(opt.getOptionText())
                            .voteCount(opt.getVoteCount())
                            .percentage(totalVotes > 0 ? Math.round(opt.getVoteCount() * 1000.0 / totalVotes) / 10.0 : 0)
                            .selected(userPollVoteOptionId != null && opt.getId().equals(userPollVoteOptionId))
                            .build())
                    .collect(Collectors.toList());
        }

        return AnnouncementResponseDto.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .priority(a.getPriority())
                .status(a.getStatus())
                .targetRole(a.getTargetRole())
                .startAt(a.getStartAt())
                .endAt(a.getEndAt())
                .pinned(a.isPinned())
                .enableComments(a.isEnableComments())
                .enableVoting(a.isEnableVoting())
                .createdByUsername(a.getCreatedBy() != null ? a.getCreatedBy().getUsername() : null)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .read(isRead)
                .acknowledged(isAcknowledged)
                .likeCount(a.getLikeCount())
                .dislikeCount(a.getDislikeCount())
                .score(a.getLikeCount() - a.getDislikeCount())
                .userVote(userVote)
                .pollOptions(pollDtos)
                .userPollVoteOptionId(userPollVoteOptionId)
                .build();
    }
}
