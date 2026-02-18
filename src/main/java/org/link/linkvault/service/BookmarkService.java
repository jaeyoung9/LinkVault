package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.BookmarkRequestDto;
import org.link.linkvault.dto.BookmarkResponseDto;
import org.link.linkvault.entity.Bookmark;
import org.link.linkvault.entity.Comment;
import org.link.linkvault.entity.Folder;
import org.link.linkvault.entity.PostPhoto;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.Tag;
import org.link.linkvault.entity.User;
import org.link.linkvault.exception.DuplicateUrlException;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.BookmarkRepository;
import org.link.linkvault.repository.CommentRepository;
import org.link.linkvault.repository.CommentVoteRepository;
import org.link.linkvault.repository.FolderRepository;
import org.link.linkvault.repository.PostPhotoRepository;
import org.link.linkvault.repository.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final TagRepository tagRepository;
    private final FolderRepository folderRepository;
    private final PostPhotoRepository postPhotoRepository;
    private final CommentRepository commentRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final MetadataExtractor metadataExtractor;
    private final AuditLogService auditLogService;
    private final FileVaultService fileVaultService;

    private boolean isAdmin(User user) {
        return user.getRole() == Role.SUPER_ADMIN
                || user.getRole() == Role.COMMUNITY_ADMIN
                || user.getRole() == Role.MODERATOR;
    }

    // --- Paginated listing (all bookmarks visible to all users) ---

    public Page<BookmarkResponseDto> findAll(User currentUser, Pageable pageable) {
        return bookmarkRepository.findAllWithTagsAndFolder(pageable)
                .map(BookmarkResponseDto::from);
    }

    public List<BookmarkResponseDto> findAll(User currentUser) {
        return bookmarkRepository.findAllWithTagsAndFolder().stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    public BookmarkResponseDto findById(Long id, User currentUser) {
        Bookmark bookmark = bookmarkRepository.findWithTagsAndFolderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + id));

        if (bookmark.isPrivatePost()) {
            boolean isOwner = currentUser != null && bookmark.getUser() != null
                    && bookmark.getUser().getId().equals(currentUser.getId());
            boolean isAdminUser = currentUser != null && isAdmin(currentUser);
            if (!isOwner && !isAdminUser) {
                throw new SecurityException("Access denied");
            }
        }

        return BookmarkResponseDto.from(bookmark);
    }

    // --- Create with duplicate detection & metadata extraction ---

    @Transactional
    public BookmarkResponseDto create(User currentUser, BookmarkRequestDto requestDto, List<MultipartFile> photos) {
        String url = requestDto.getUrl();
        boolean hasUrl = url != null && !url.isBlank();

        if (hasUrl && bookmarkRepository.existsByUrlAndUserId(url, currentUser.getId())) {
            throw new DuplicateUrlException("Bookmark already exists with URL: " + url);
        }

        Folder folder = resolveFolder(currentUser, requestDto.getFolderId());

        String title = requestDto.getTitle();
        String description = requestDto.getDescription();
        String favicon = "";

        // Extract metadata from URL only if URL is provided
        if (hasUrl) {
            Map<String, String> metadata = metadataExtractor.extract(url);
            if (title == null || title.isBlank()) {
                title = metadata.getOrDefault("title", url);
            }
            if (description == null || description.isBlank()) {
                description = metadata.getOrDefault("description", "");
            }
            favicon = metadata.getOrDefault("favicon", "");
        }

        Bookmark bookmark = Bookmark.builder()
                .title(title)
                .url(hasUrl ? url : null)
                .description(description)
                .favicon(favicon)
                .folder(folder)
                .user(currentUser)
                .latitude(requestDto.getLatitude())
                .longitude(requestDto.getLongitude())
                .address(requestDto.getAddress())
                .caption(requestDto.getCaption())
                .mapEmoji(requestDto.getMapEmoji())
                .privatePost(Boolean.TRUE.equals(requestDto.getPrivatePost()))
                .build();

        assignTags(bookmark, requestDto.getTagNames());

        Bookmark saved = bookmarkRepository.save(bookmark);

        // Process photos (max 4)
        processPhotos(saved, photos);

        auditLogService.log(currentUser.getUsername(), AuditActionCodes.BOOKMARK_CREATE, "Bookmark", saved.getId(), title);
        return BookmarkResponseDto.from(saved);
    }

    // Backward-compatible create without photos
    @Transactional
    public BookmarkResponseDto create(User currentUser, BookmarkRequestDto requestDto) {
        return create(currentUser, requestDto, null);
    }

    @Transactional
    public BookmarkResponseDto update(User currentUser, Long id, BookmarkRequestDto requestDto,
                                       List<MultipartFile> newPhotos, List<Long> deletePhotoIds, List<Long> photoOrder) {
        BookmarkResponseDto result = updateInternal(currentUser, id, requestDto, newPhotos, deletePhotoIds);

        // Reorder photos if photoOrder provided
        if (photoOrder != null && !photoOrder.isEmpty()) {
            Bookmark bookmark = bookmarkRepository.findById(id).orElse(null);
            if (bookmark != null) {
                int order = 0;
                for (Long photoId : photoOrder) {
                    for (PostPhoto photo : bookmark.getPhotos()) {
                        if (photo.getId().equals(photoId)) {
                            photo.setDisplayOrder(order++);
                            break;
                        }
                    }
                }
                // Assign order for any new photos not in the order list
                for (PostPhoto photo : bookmark.getPhotos()) {
                    if (photo.getId() == null || !photoOrder.contains(photo.getId())) {
                        photo.setDisplayOrder(order++);
                    }
                }
            }
        }

        return result;
    }

    @Transactional
    public BookmarkResponseDto update(User currentUser, Long id, BookmarkRequestDto requestDto,
                                       List<MultipartFile> newPhotos, List<Long> deletePhotoIds) {
        return updateInternal(currentUser, id, requestDto, newPhotos, deletePhotoIds);
    }

    private BookmarkResponseDto updateInternal(User currentUser, Long id, BookmarkRequestDto requestDto,
                                       List<MultipartFile> newPhotos, List<Long> deletePhotoIds) {
        Bookmark bookmark = bookmarkRepository.findWithTagsAndFolderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + id));

        // Verify ownership
        if (!isAdmin(currentUser) && (bookmark.getUser() == null || !bookmark.getUser().getId().equals(currentUser.getId()))) {
            throw new SecurityException("Access denied");
        }

        String newUrl = requestDto.getUrl();
        boolean hasNewUrl = newUrl != null && !newUrl.isBlank();
        String oldUrl = bookmark.getUrl();

        // Check URL uniqueness if changed (null-safe comparison)
        if (hasNewUrl && !newUrl.equals(oldUrl)) {
            Long ownerId = bookmark.getUser() != null ? bookmark.getUser().getId() : currentUser.getId();
            if (bookmarkRepository.existsByUrlAndUserId(newUrl, ownerId)) {
                throw new DuplicateUrlException("Another bookmark already exists with URL: " + newUrl);
            }
        }

        bookmark.update(requestDto.getTitle(), hasNewUrl ? newUrl : null, requestDto.getDescription(),
                requestDto.getLatitude(), requestDto.getLongitude(), requestDto.getAddress(), requestDto.getCaption(), requestDto.getMapEmoji());
        bookmark.setFolder(resolveFolder(currentUser, requestDto.getFolderId()));
        bookmark.setPrivatePost(Boolean.TRUE.equals(requestDto.getPrivatePost()));

        // Refresh favicon if URL changed
        if (hasNewUrl && !newUrl.equals(oldUrl)) {
            Map<String, String> metadata = metadataExtractor.extract(newUrl);
            bookmark.setFavicon(metadata.getOrDefault("favicon", ""));
        }

        // Clear existing tags and reassign
        new HashSet<>(bookmark.getTags()).forEach(bookmark::removeTag);
        assignTags(bookmark, requestDto.getTagNames());

        // Handle photo deletions
        if (deletePhotoIds != null && !deletePhotoIds.isEmpty()) {
            List<PostPhoto> toDelete = postPhotoRepository.findByBookmarkId(id).stream()
                    .filter(p -> deletePhotoIds.contains(p.getId()))
                    .collect(Collectors.toList());
            for (PostPhoto photo : toDelete) {
                fileVaultService.delete(photo.getStoragePath());
                bookmark.removePhoto(photo);
            }
        }

        // Handle new photo additions
        processPhotos(bookmark, newPhotos);

        auditLogService.log(currentUser.getUsername(), AuditActionCodes.BOOKMARK_UPDATE, "Bookmark", id, requestDto.getTitle());
        return BookmarkResponseDto.from(bookmark);
    }

    // Backward-compatible update without photos
    @Transactional
    public BookmarkResponseDto update(User currentUser, Long id, BookmarkRequestDto requestDto) {
        return update(currentUser, id, requestDto, null, null);
    }

    @Transactional
    public void delete(User currentUser, Long id) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + id));

        // Verify ownership
        if (!isAdmin(currentUser) && (bookmark.getUser() == null || !bookmark.getUser().getId().equals(currentUser.getId()))) {
            throw new SecurityException("Access denied");
        }

        bookmark.softDelete();
        auditLogService.log(currentUser.getUsername(), AuditActionCodes.BOOKMARK_SOFT_DELETE, "Bookmark", id, bookmark.getTitle());
    }

    @Transactional
    public void restore(User currentUser, Long id) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + id));
        bookmark.restore();
        auditLogService.log(currentUser.getUsername(), AuditActionCodes.BOOKMARK_RESTORE, "Bookmark", id, bookmark.getTitle());
    }

    public Page<BookmarkResponseDto> findAllDeleted(Pageable pageable) {
        return bookmarkRepository.findAllDeletedBookmarks(pageable)
                .map(BookmarkResponseDto::from);
    }

    @Transactional
    public void purge(User currentUser, Long id) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + id));
        if (!bookmark.isDeleted()) {
            throw new IllegalStateException("Cannot purge a bookmark that is not soft-deleted");
        }

        // 1. Delete comment votes for each comment on this bookmark
        List<Comment> comments = commentRepository.findAllByBookmarkId(id);
        for (Comment comment : comments) {
            commentVoteRepository.deleteByCommentId(comment.getId());
        }

        // 2. Detach child comments and delete all comments on this bookmark
        commentRepository.deleteByBookmarkId(id);

        // 3. Delete photo files from disk
        if (bookmark.getPhotos() != null) {
            for (PostPhoto photo : bookmark.getPhotos()) {
                fileVaultService.delete(photo.getStoragePath());
            }
        }

        // 4. Delete bookmark only after dependencies are cleaned
        bookmarkRepository.delete(bookmark);

        // 5. Audit log after successful purge
        auditLogService.log(currentUser.getUsername(), AuditActionCodes.BOOKMARK_PURGE, "Bookmark", id, bookmark.getTitle());
    }

    // --- Access tracking ---

    @Transactional
    public BookmarkResponseDto recordAccess(Long id, User currentUser) {
        Bookmark bookmark = findByIdWithAccessControl(id, currentUser);
        bookmark.recordAccess();
        return BookmarkResponseDto.from(bookmark);
    }

    @Transactional(readOnly = true)
    public BookmarkResponseDto findByIdForUser(Long id, User currentUser) {
        Bookmark bookmark = findByIdWithAccessControl(id, currentUser);
        return BookmarkResponseDto.from(bookmark);
    }

    private Bookmark findByIdWithAccessControl(Long id, User currentUser) {
        Bookmark bookmark = bookmarkRepository.findWithTagsAndFolderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + id));

        if (bookmark.isPrivatePost()) {
            boolean isOwner = currentUser != null && bookmark.getUser() != null
                    && bookmark.getUser().getId().equals(currentUser.getId());
            boolean isAdminUser = currentUser != null && isAdmin(currentUser);
            if (!isOwner && !isAdminUser) {
                throw new SecurityException("Access denied");
            }
        }
        return bookmark;
    }

    public List<BookmarkResponseDto> findFrequentlyAccessed(User currentUser, int limit) {
        return bookmarkRepository.findTopByAccessCountAndUserId(currentUser.getId(), PageRequest.of(0, limit)).stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<BookmarkResponseDto> findRecentlyAccessed(int limit) {
        return bookmarkRepository.findRecentlyAccessed(PageRequest.of(0, limit)).stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    // --- Search & filter ---

    public Page<BookmarkResponseDto> searchByKeyword(User currentUser, String keyword, Pageable pageable) {
        return bookmarkRepository.searchByKeyword(keyword, pageable)
                .map(BookmarkResponseDto::from);
    }

    public List<BookmarkResponseDto> findByTagName(User currentUser, String tagName) {
        return bookmarkRepository.findByTagName(tagName).stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<BookmarkResponseDto> findByFolderId(User currentUser, Long folderId) {
        return bookmarkRepository.findByUserIdAndFolderId(currentUser.getId(), folderId).stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<BookmarkResponseDto> findUncategorized(User currentUser) {
        return bookmarkRepository.findByUserIdAndFolderIsNull(currentUser.getId()).stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    // --- Public access (guest) ---

    public Page<BookmarkResponseDto> findAllPublic(Pageable pageable) {
        return bookmarkRepository.findAllPublic(pageable)
                .map(BookmarkResponseDto::from);
    }

    public BookmarkResponseDto findByIdPublic(Long id) {
        Bookmark bookmark = bookmarkRepository.findByIdPublic(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + id));
        return BookmarkResponseDto.from(bookmark);
    }

    public Page<BookmarkResponseDto> searchByKeywordPublic(String keyword, Pageable pageable) {
        return bookmarkRepository.searchByKeywordPublic(keyword, pageable)
                .map(BookmarkResponseDto::from);
    }

    public List<BookmarkResponseDto> findByTagNamePublic(String tagName) {
        return bookmarkRepository.findByTagNamePublic(tagName).stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    // --- Admin listing (includes private posts) ---

    public Page<BookmarkResponseDto> findAllForAdmin(Pageable pageable) {
        return bookmarkRepository.findAllIncludingPrivate(pageable)
                .map(BookmarkResponseDto::from);
    }

    // --- Private posts ---

    public List<BookmarkResponseDto> findPrivateByUser(User user) {
        return bookmarkRepository.findPrivateByUserId(user.getId()).stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    // --- Map discovery ---

    public List<BookmarkResponseDto> findAllWithLocation() {
        return bookmarkRepository.findAllWithLocation().stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<BookmarkResponseDto> findAllWithLocationAdmin() {
        return bookmarkRepository.findAllWithLocationAdmin().stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    // --- Move bookmark to folder (drag-and-drop) ---

    @Transactional
    public BookmarkResponseDto moveToFolder(User currentUser, Long bookmarkId, Long folderId) {
        Bookmark bookmark = bookmarkRepository.findWithTagsAndFolderById(bookmarkId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + bookmarkId));

        if (!isAdmin(currentUser) && (bookmark.getUser() == null || !bookmark.getUser().getId().equals(currentUser.getId()))) {
            throw new SecurityException("Access denied");
        }

        bookmark.setFolder(resolveFolder(currentUser, folderId));
        return BookmarkResponseDto.from(bookmark);
    }

    // --- Duplicate check ---

    public boolean existsByUrl(User currentUser, String url) {
        return bookmarkRepository.existsByUrlAndUserId(url, currentUser.getId());
    }

    // --- Internal helpers ---

    private void assignTags(Bookmark bookmark, Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        for (String tagName : tagNames) {
            String trimmed = tagName.trim();
            if (trimmed.isEmpty()) continue;
            Tag tag = tagRepository.findByName(trimmed)
                    .orElseGet(() -> tagRepository.save(new Tag(trimmed)));
            bookmark.addTag(tag);
        }
    }

    private Folder resolveFolder(Long folderId) {
        if (folderId == null) return null;
        return folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found with id: " + folderId));
    }

    private Folder resolveFolder(User user, Long folderId) {
        if (folderId == null) return null;
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found with id: " + folderId));
        if (!isAdmin(user) && (folder.getUser() == null || !folder.getUser().getId().equals(user.getId()))) {
            throw new SecurityException("Access denied");
        }
        return folder;
    }

    private void processPhotos(Bookmark bookmark, List<MultipartFile> photos) {
        if (photos == null || photos.isEmpty()) return;

        int currentCount = bookmark.getPhotos() != null ? bookmark.getPhotos().size() : 0;
        int maxPhotos = 4;

        for (int i = 0; i < photos.size() && (currentCount + i) < maxPhotos; i++) {
            MultipartFile file = photos.get(i);
            if (file.isEmpty()) continue;
            try {
                String storagePath = fileVaultService.store(file);
                PostPhoto postPhoto = PostPhoto.builder()
                        .storagePath(storagePath)
                        .originalFilename(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .contentType(file.getContentType())
                        .displayOrder(currentCount + i)
                        .build();
                bookmark.addPhoto(postPhoto);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store photo: " + file.getOriginalFilename(), e);
            }
        }
    }
}
