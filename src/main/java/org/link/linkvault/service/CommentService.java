package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.CommentRequestDto;
import org.link.linkvault.dto.CommentResponseDto;
import org.link.linkvault.dto.CommentVoteResponseDto;
import org.link.linkvault.entity.*;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.AnnouncementRepository;
import org.link.linkvault.repository.BookmarkRepository;
import org.link.linkvault.repository.CommentRepository;
import org.link.linkvault.repository.CommentVoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final int MAX_DEPTH = 5;

    private final CommentRepository commentRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final BookmarkRepository bookmarkRepository;
    private final AnnouncementRepository announcementRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public List<CommentResponseDto> getCommentsForBookmark(Long bookmarkId, User currentUser) {
        List<Comment> allComments = commentRepository.findAllByBookmarkId(bookmarkId);

        // Batch load user votes
        List<Long> commentIds = allComments.stream().map(Comment::getId).collect(Collectors.toList());
        Map<Long, VoteType> userVotes = new HashMap<>();
        if (currentUser != null && !commentIds.isEmpty()) {
            commentVoteRepository.findByUserIdAndCommentIdIn(currentUser.getId(), commentIds)
                    .forEach(v -> userVotes.put(v.getComment().getId(), v.getVoteType()));
        }

        // Build tree from flat list
        Map<Long, List<Comment>> childrenMap = new HashMap<>();
        List<Comment> rootComments = new ArrayList<>();

        for (Comment comment : allComments) {
            if (comment.getParent() == null) {
                rootComments.add(comment);
            } else {
                childrenMap.computeIfAbsent(comment.getParent().getId(), k -> new ArrayList<>()).add(comment);
            }
        }

        return rootComments.stream()
                .map(c -> buildCommentTree(c, childrenMap, userVotes, currentUser))
                .collect(Collectors.toList());
    }

    public List<CommentResponseDto> getCommentsForAnnouncement(Long announcementId, User currentUser) {
        List<Comment> allComments = commentRepository.findAllByAnnouncementId(announcementId);

        List<Long> commentIds = allComments.stream().map(Comment::getId).collect(Collectors.toList());
        Map<Long, VoteType> userVotes = new HashMap<>();
        if (currentUser != null && !commentIds.isEmpty()) {
            commentVoteRepository.findByUserIdAndCommentIdIn(currentUser.getId(), commentIds)
                    .forEach(v -> userVotes.put(v.getComment().getId(), v.getVoteType()));
        }

        Map<Long, List<Comment>> childrenMap = new HashMap<>();
        List<Comment> rootComments = new ArrayList<>();

        for (Comment comment : allComments) {
            if (comment.getParent() == null) {
                rootComments.add(comment);
            } else {
                childrenMap.computeIfAbsent(comment.getParent().getId(), k -> new ArrayList<>()).add(comment);
            }
        }

        return rootComments.stream()
                .map(c -> buildCommentTree(c, childrenMap, userVotes, currentUser))
                .collect(Collectors.toList());
    }

    private CommentResponseDto buildCommentTree(Comment comment, Map<Long, List<Comment>> childrenMap,
                                                  Map<Long, VoteType> userVotes, User currentUser) {
        List<Comment> children = childrenMap.getOrDefault(comment.getId(), Collections.emptyList());
        List<CommentResponseDto> childDtos = children.stream()
                .map(c -> buildCommentTree(c, childrenMap, userVotes, currentUser))
                .collect(Collectors.toList());

        boolean canEdit = currentUser != null && !comment.isDeleted() &&
                comment.getUser().getId().equals(currentUser.getId());
        boolean canDelete = currentUser != null && !comment.isDeleted() &&
                comment.getUser().getId().equals(currentUser.getId());

        String parentUsername = null;
        if (comment.getParent() != null) {
            parentUsername = comment.getParent().isDeleted()
                    ? "[deleted]"
                    : comment.getParent().getUser().getUsername();
        }

        int replyCount = countAllReplies(comment.getId(), childrenMap);

        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .username(comment.getUser().getUsername())
                .userId(comment.getUser().getId())
                .bookmarkId(comment.getBookmark() != null ? comment.getBookmark().getId() : null)
                .announcementId(comment.getAnnouncement() != null ? comment.getAnnouncement().getId() : null)
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .parentUsername(parentUsername)
                .depth(comment.getDepth())
                .likeCount(comment.getLikeCount())
                .dislikeCount(comment.getDislikeCount())
                .score(comment.getLikeCount() - comment.getDislikeCount())
                .replyCount(replyCount)
                .deleted(comment.isDeleted())
                .edited(comment.isEdited())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .userVote(userVotes.get(comment.getId()))
                .canEdit(canEdit)
                .canDelete(canDelete)
                .replies(childDtos)
                .build();
    }

    private int countAllReplies(Long commentId, Map<Long, List<Comment>> childrenMap) {
        List<Comment> direct = childrenMap.getOrDefault(commentId, Collections.emptyList());
        int count = direct.size();
        for (Comment child : direct) {
            count += countAllReplies(child.getId(), childrenMap);
        }
        return count;
    }

    @Transactional
    public CommentResponseDto create(CommentRequestDto dto, User user) {
        if (dto.getBookmarkId() == null && dto.getAnnouncementId() == null) {
            throw new IllegalArgumentException("Either bookmarkId or announcementId is required");
        }

        Bookmark bookmark = null;
        Announcement announcement = null;

        if (dto.getAnnouncementId() != null) {
            announcement = announcementRepository.findById(dto.getAnnouncementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + dto.getAnnouncementId()));
        } else {
            bookmark = bookmarkRepository.findById(dto.getBookmarkId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found: " + dto.getBookmarkId()));
        }

        Comment parent = null;
        int depth = 0;

        if (dto.getParentId() != null) {
            parent = commentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found: " + dto.getParentId()));
            depth = parent.getDepth() + 1;
            if (depth > MAX_DEPTH) {
                throw new IllegalArgumentException("Maximum comment depth exceeded");
            }
        }

        Comment comment = Comment.builder()
                .content(dto.getContent())
                .user(user)
                .bookmark(bookmark)
                .announcement(announcement)
                .parent(parent)
                .depth(depth)
                .build();

        comment = commentRepository.save(comment);
        if (bookmark != null) {
            bookmark.incrementCommentCount();
        }

        // Trigger notifications
        if (comment.getParent() != null) {
            notificationService.notifyReply(comment, user);
        } else if (bookmark != null) {
            notificationService.notifyComment(comment, user);
        }
        notificationService.notifyMentions(comment, user);

        return CommentResponseDto.from(comment);
    }

    @Transactional
    public CommentResponseDto update(Long commentId, String content, User user, boolean isModerator) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        if (!isModerator && !comment.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized to edit this comment");
        }

        comment.updateContent(content);
        return CommentResponseDto.from(comment);
    }

    @Transactional
    public void delete(Long commentId, User user, boolean isModerator) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        if (!isModerator && !comment.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized to delete this comment");
        }

        comment.softDelete();
        if (comment.getBookmark() != null) {
            comment.getBookmark().decrementCommentCount();
        }

        if (isModerator) {
            String targetId = comment.getBookmark() != null
                    ? String.valueOf(comment.getBookmark().getId())
                    : (comment.getAnnouncement() != null ? "ann:" + comment.getAnnouncement().getId() : "unknown");
            auditLogService.log(user.getUsername(), AuditActionCodes.COMMENT_SOFT_DELETE, "Comment", commentId,
                    AuditDetailFormatter.format("targetId", targetId,
                            "owner", comment.getUser().getUsername(), "moderatorAction", "true"));
        }
    }

    @Transactional
    public CommentVoteResponseDto vote(Long commentId, VoteType voteType, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        Optional<CommentVote> existingVote = commentVoteRepository.findByUserIdAndCommentId(user.getId(), commentId);

        VoteType resultVote = null;

        if (existingVote.isPresent()) {
            CommentVote vote = existingVote.get();
            if (vote.getVoteType() == voteType) {
                // Same vote: remove it
                if (voteType == VoteType.LIKE) comment.decrementLikeCount();
                else comment.decrementDislikeCount();
                commentVoteRepository.delete(vote);
            } else {
                // Different vote: switch
                if (vote.getVoteType() == VoteType.LIKE) {
                    comment.decrementLikeCount();
                    comment.incrementDislikeCount();
                } else {
                    comment.decrementDislikeCount();
                    comment.incrementLikeCount();
                }
                vote.changeVoteType(voteType);
                resultVote = voteType;
            }
        } else {
            // New vote
            if (voteType == VoteType.LIKE) comment.incrementLikeCount();
            else comment.incrementDislikeCount();
            commentVoteRepository.save(new CommentVote(user, comment, voteType));
            resultVote = voteType;
            if (voteType == VoteType.LIKE) {
                notificationService.notifyVote(comment, user);
            }
        }

        return CommentVoteResponseDto.builder()
                .likeCount(comment.getLikeCount())
                .dislikeCount(comment.getDislikeCount())
                .score(comment.getLikeCount() - comment.getDislikeCount())
                .userVote(resultVote)
                .build();
    }

    public List<CommentResponseDto> getAllComments() {
        return commentRepository.findAllWithUserAndBookmark().stream()
                .map(CommentResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void restoreComment(Long commentId, String actorUsername) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));
        if (!comment.isDeleted()) {
            throw new IllegalStateException("Comment is not deleted");
        }
        comment.restore();
        if (comment.getBookmark() != null) {
            comment.getBookmark().incrementCommentCount();
        }

        String targetId = comment.getBookmark() != null
                ? String.valueOf(comment.getBookmark().getId())
                : (comment.getAnnouncement() != null ? "ann:" + comment.getAnnouncement().getId() : "unknown");
        auditLogService.log(actorUsername, AuditActionCodes.COMMENT_RESTORE, "Comment", commentId,
                AuditDetailFormatter.format("targetId", targetId,
                        "owner", comment.getUser().getUsername()));
    }

    @Transactional
    public void purgeComment(Long commentId, String actorUsername) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));
        if (!comment.isDeleted()) {
            throw new IllegalStateException("Cannot purge a comment that is not soft-deleted");
        }

        // 1. Delete votes on this comment
        commentVoteRepository.deleteByCommentId(commentId);

        // 2. Detach child comments (preserve thread, set parent to null)
        commentRepository.detachRepliesFromComment(commentId);

        // 3. Delete the comment
        commentRepository.delete(comment);

        String purgeTargetId = comment.getBookmark() != null
                ? String.valueOf(comment.getBookmark().getId())
                : (comment.getAnnouncement() != null ? "ann:" + comment.getAnnouncement().getId() : "unknown");
        auditLogService.log(actorUsername, AuditActionCodes.COMMENT_PURGE, "Comment", commentId,
                AuditDetailFormatter.format("targetId", purgeTargetId,
                        "owner", comment.getUser().getUsername()));
    }
}
