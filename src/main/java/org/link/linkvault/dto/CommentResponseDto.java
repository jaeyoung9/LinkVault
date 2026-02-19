package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.Comment;
import org.link.linkvault.entity.VoteType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CommentResponseDto {

    private Long id;
    private String content;
    private String username;
    private Long userId;
    private Long bookmarkId;
    private Long announcementId;
    private Long parentId;
    private String parentUsername;
    private int depth;
    private int likeCount;
    private int dislikeCount;
    private int score;
    private int replyCount;
    private boolean deleted;
    private boolean edited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private VoteType userVote;
    private boolean canEdit;
    private boolean canDelete;
    private List<CommentResponseDto> replies;

    public static CommentResponseDto from(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .username(comment.getUser().getUsername())
                .userId(comment.getUser().getId())
                .bookmarkId(comment.getBookmark() != null ? comment.getBookmark().getId() : null)
                .announcementId(comment.getAnnouncement() != null ? comment.getAnnouncement().getId() : null)
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .depth(comment.getDepth())
                .likeCount(comment.getLikeCount())
                .dislikeCount(comment.getDislikeCount())
                .score(comment.getLikeCount() - comment.getDislikeCount())
                .deleted(comment.isDeleted())
                .edited(comment.isEdited())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
