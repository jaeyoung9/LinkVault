package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.UserRequestDto;
import org.link.linkvault.dto.UserResponseDto;
import org.link.linkvault.entity.*;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.link.linkvault.entity.PrivacyPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CommentVoteRepository commentVoteRepository;
    private final CommentRepository commentRepository;
    private final SavedBookmarkRepository savedBookmarkRepository;
    private final FavoriteBookmarkRepository favoriteBookmarkRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final QnaFeedbackRepository qnaFeedbackRepository;
    private final InvitationUseRepository invitationUseRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final BookmarkRepository bookmarkRepository;
    private final FolderRepository folderRepository;
    private final AnnouncementRepository announcementRepository;
    private final QnaArticleRepository qnaArticleRepository;
    private final InvitationCodeRepository invitationCodeRepository;
    private final PrivacyPolicyRepository privacyPolicyRepository;

    public List<UserResponseDto> findAll() {
        return userRepository.findAll().stream()
                .map(user -> UserResponseDto.from(user, userRepository.countBookmarksByUserId(user.getId())))
                .collect(Collectors.toList());
    }

    public UserResponseDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponseDto.from(user, userRepository.countBookmarksByUserId(user.getId()));
    }

    public User getUserEntity(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional
    public UserResponseDto create(UserRequestDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + dto.getUsername());
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + dto.getEmail());
        }

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .build();

        user = userRepository.save(user);
        return UserResponseDto.from(user, 0);
    }

    @Transactional
    public UserResponseDto update(Long id, UserRequestDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check email uniqueness if changed
        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + dto.getEmail());
        }

        user.updateProfile(dto.getEmail());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.updatePassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getRole() != null) {
            user.updateRole(dto.getRole());
        }

        if (dto.getEnabled() != null) {
            user.setEnabled(dto.getEnabled());
        }

        return UserResponseDto.from(user, userRepository.countBookmarksByUserId(user.getId()));
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        Long userId = user.getId();

        // 1. Delete comment votes cast by this user
        commentVoteRepository.deleteByUserId(userId);

        // 2. Clean up bookmarks owned by this user
        List<Bookmark> userBookmarks = bookmarkRepository.findAllByUserId(userId);
        for (Bookmark bookmark : userBookmarks) {
            Long bookmarkId = bookmark.getId();
            // Delete votes on comments on this bookmark
            commentRepository.findAllByBookmarkId(bookmarkId)
                    .forEach(c -> commentVoteRepository.deleteByCommentId(c.getId()));
            // Delete comments on this bookmark
            commentRepository.deleteByBookmarkId(bookmarkId);
            // Delete saved/favorite records referencing this bookmark
            savedBookmarkRepository.deleteByBookmarkId(bookmarkId);
            favoriteBookmarkRepository.deleteByBookmarkId(bookmarkId);
            // Clear ManyToMany tags
            bookmark.getTags().clear();
        }
        bookmarkRepository.deleteAll(userBookmarks);

        // 3. Handle user's comments on other users' bookmarks
        commentRepository.detachRepliesFromUserComments(userId);
        commentRepository.findByUserId(userId)
                .forEach(c -> commentVoteRepository.deleteByCommentId(c.getId()));
        commentRepository.deleteByUserId(userId);

        // 4. Delete user's saved/favorite bookmarks (on other users' bookmarks)
        savedBookmarkRepository.deleteByUserId(userId);
        favoriteBookmarkRepository.deleteByUserId(userId);

        // 5. Delete notifications
        notificationRepository.deleteByRecipientId(userId);
        notificationRepository.deleteBySourceUserId(userId);

        // 6. Delete audit logs
        auditLogRepository.deleteByUserId(userId);

        // 7. Delete announcement reads
        announcementReadRepository.deleteByUserId(userId);

        // 8. Delete QnA feedback
        qnaFeedbackRepository.deleteByUserId(userId);

        // 9. Delete invitation uses
        invitationUseRepository.deleteByUserId(userId);

        // 10. Delete user settings
        userSettingsRepository.deleteByUserId(userId);

        // 11. Handle announcements created by this user
        List<Announcement> userAnnouncements = announcementRepository.findByCreatedById(userId);
        for (Announcement a : userAnnouncements) {
            announcementReadRepository.deleteByAnnouncementId(a.getId());
        }
        announcementRepository.deleteAll(userAnnouncements);

        // 12. Handle QnA articles created by this user
        List<QnaArticle> userArticles = qnaArticleRepository.findByCreatedById(userId);
        for (QnaArticle q : userArticles) {
            qnaFeedbackRepository.deleteByQnaArticleId(q.getId());
        }
        qnaArticleRepository.deleteAll(userArticles);

        // 13. Handle invitation codes created by this user
        List<InvitationCode> userCodes = invitationCodeRepository.findByCreatedById(userId);
        for (InvitationCode ic : userCodes) {
            invitationUseRepository.deleteByInvitationCodeId(ic.getId());
        }
        invitationCodeRepository.deleteAll(userCodes);

        // 14. Delete folders (cascade handles children)
        List<Folder> rootFolders = folderRepository.findRootFoldersByUserId(userId);
        folderRepository.deleteAll(rootFolders);

        // 15. Delete user
        userRepository.delete(user);
    }

    @Transactional
    public UserResponseDto toggleEnabled(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setEnabled(!user.isEnabled());
        return UserResponseDto.from(user, userRepository.countBookmarksByUserId(user.getId()));
    }

    @Transactional
    public void recordLogin(String username) {
        userRepository.findByUsername(username).ifPresent(User::recordLogin);
    }

    @Transactional
    public int bulkDeactivateNonConsented(String reason) {
        PrivacyPolicy activePolicy = privacyPolicyRepository.findByActiveTrue().orElse(null);
        if (activePolicy == null) {
            return 0;
        }

        List<User> nonConsentedUsers = userRepository.findEnabledUsersNotConsentedToVersion(activePolicy.getVersion());
        if (nonConsentedUsers.isEmpty()) {
            return 0;
        }

        for (User user : nonConsentedUsers) {
            user.deactivateForPrivacy(reason + " (policy v" + activePolicy.getVersion() + ")");
        }
        userRepository.saveAll(nonConsentedUsers);
        return nonConsentedUsers.size();
    }
}
