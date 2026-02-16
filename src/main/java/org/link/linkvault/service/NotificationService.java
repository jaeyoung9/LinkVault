package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.NotificationResponseDto;
import org.link.linkvault.entity.*;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.NotificationRepository;
import org.link.linkvault.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(User recipient, User sourceUser, NotificationType type,
                                   String message, Long bookmarkId, Long commentId) {
        if (recipient.getId().equals(sourceUser.getId())) {
            return; // skip self-notifications
        }
        Notification notification = Notification.builder()
                .recipient(recipient)
                .sourceUser(sourceUser)
                .type(type)
                .message(message)
                .relatedBookmarkId(bookmarkId)
                .relatedCommentId(commentId)
                .build();
        notificationRepository.save(notification);
    }

    public Page<NotificationResponseDto> getNotifications(User user, Pageable pageable) {
        return notificationRepository.findByRecipientId(user.getId(), pageable)
                .map(NotificationResponseDto::from);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByRecipientIdAndReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new SecurityException("Not authorized");
        }
        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user.getId());
    }

    @Transactional
    public void notifyReply(Comment comment, User commenter) {
        Comment parent = comment.getParent();
        if (parent == null || parent.isDeleted()) return;
        User parentAuthor = parent.getUser();
        String message = commenter.getUsername() + " replied to your comment";
        createNotification(parentAuthor, commenter, NotificationType.REPLY, message,
                comment.getBookmark().getId(), comment.getId());
    }

    @Transactional
    public void notifyMentions(Comment comment, User commenter) {
        Matcher matcher = MENTION_PATTERN.matcher(comment.getContent());
        while (matcher.find()) {
            String mentionedUsername = matcher.group(1);
            userRepository.findByUsername(mentionedUsername).ifPresent(mentioned -> {
                String message = commenter.getUsername() + " mentioned you in a comment";
                createNotification(mentioned, commenter, NotificationType.MENTION, message,
                        comment.getBookmark().getId(), comment.getId());
            });
        }
    }

    @Transactional
    public void notifyVote(Comment comment, User voter) {
        if (comment.isDeleted()) return;
        User author = comment.getUser();
        String message = voter.getUsername() + " liked your comment";
        createNotification(author, voter, NotificationType.VOTE, message,
                comment.getBookmark().getId(), comment.getId());
    }

    @Transactional
    public void notifyComment(Comment comment, User commenter) {
        Bookmark bookmark = comment.getBookmark();
        User owner = bookmark.getUser();
        String message = commenter.getUsername() + " commented on your bookmark \"" + bookmark.getTitle() + "\"";
        createNotification(owner, commenter, NotificationType.COMMENT, message,
                bookmark.getId(), comment.getId());
    }

    @Transactional
    public void notifySave(Bookmark bookmark, User saver) {
        User owner = bookmark.getUser();
        String message = saver.getUsername() + " saved your bookmark \"" + bookmark.getTitle() + "\"";
        createNotification(owner, saver, NotificationType.SAVE, message,
                bookmark.getId(), null);
    }

    @Transactional
    public void notifyFavorite(Bookmark bookmark, User favoriter) {
        User owner = bookmark.getUser();
        String message = favoriter.getUsername() + " favorited your bookmark \"" + bookmark.getTitle() + "\"";
        createNotification(owner, favoriter, NotificationType.FAVORITE, message,
                bookmark.getId(), null);
    }
}
