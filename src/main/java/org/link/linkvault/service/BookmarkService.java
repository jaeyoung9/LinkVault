package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.BookmarkRequestDto;
import org.link.linkvault.dto.BookmarkResponseDto;
import org.link.linkvault.entity.Bookmark;
import org.link.linkvault.entity.Folder;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.Tag;
import org.link.linkvault.entity.User;
import org.link.linkvault.exception.DuplicateUrlException;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.BookmarkRepository;
import org.link.linkvault.repository.FolderRepository;
import org.link.linkvault.repository.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final MetadataExtractor metadataExtractor;
    private final AuditLogService auditLogService;

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    // --- Paginated listing ---

    public Page<BookmarkResponseDto> findAll(User currentUser, Pageable pageable) {
        if (isAdmin(currentUser)) {
            return bookmarkRepository.findAllWithTagsAndFolder(pageable)
                    .map(BookmarkResponseDto::from);
        }
        return bookmarkRepository.findByUserId(currentUser.getId(), pageable)
                .map(BookmarkResponseDto::from);
    }

    public List<BookmarkResponseDto> findAll(User currentUser) {
        if (isAdmin(currentUser)) {
            return bookmarkRepository.findAllWithTagsAndFolder().stream()
                    .map(BookmarkResponseDto::from)
                    .collect(Collectors.toList());
        }
        return bookmarkRepository.findByUserId(currentUser.getId(), Pageable.unpaged()).stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    public BookmarkResponseDto findById(Long id) {
        Bookmark bookmark = bookmarkRepository.findWithTagsAndFolderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + id));
        return BookmarkResponseDto.from(bookmark);
    }

    // --- Create with duplicate detection & metadata extraction ---

    @Transactional
    public BookmarkResponseDto create(User currentUser, BookmarkRequestDto requestDto) {
        if (bookmarkRepository.existsByUrlAndUserId(requestDto.getUrl(), currentUser.getId())) {
            throw new DuplicateUrlException("Bookmark already exists with URL: " + requestDto.getUrl());
        }

        Folder folder = resolveFolder(requestDto.getFolderId());

        // Extract metadata from URL
        Map<String, String> metadata = metadataExtractor.extract(requestDto.getUrl());
        String title = (requestDto.getTitle() != null && !requestDto.getTitle().isBlank())
                ? requestDto.getTitle()
                : metadata.getOrDefault("title", requestDto.getUrl());
        String description = (requestDto.getDescription() != null && !requestDto.getDescription().isBlank())
                ? requestDto.getDescription()
                : metadata.getOrDefault("description", "");

        Bookmark bookmark = Bookmark.builder()
                .title(title)
                .url(requestDto.getUrl())
                .description(description)
                .favicon(metadata.getOrDefault("favicon", ""))
                .folder(folder)
                .user(currentUser)
                .build();

        assignTags(bookmark, requestDto.getTagNames());

        Bookmark saved = bookmarkRepository.save(bookmark);
        auditLogService.log(currentUser.getUsername(), "CREATE_BOOKMARK", "Bookmark", saved.getId(), title);
        return BookmarkResponseDto.from(saved);
    }

    @Transactional
    public BookmarkResponseDto update(User currentUser, Long id, BookmarkRequestDto requestDto) {
        Bookmark bookmark = bookmarkRepository.findWithTagsAndFolderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + id));

        // Verify ownership
        if (!isAdmin(currentUser) && (bookmark.getUser() == null || !bookmark.getUser().getId().equals(currentUser.getId()))) {
            throw new SecurityException("Access denied");
        }

        // Check URL uniqueness if changed (scoped to user)
        if (!bookmark.getUrl().equals(requestDto.getUrl())) {
            Long ownerId = bookmark.getUser() != null ? bookmark.getUser().getId() : currentUser.getId();
            if (bookmarkRepository.existsByUrlAndUserId(requestDto.getUrl(), ownerId)) {
                throw new DuplicateUrlException("Another bookmark already exists with URL: " + requestDto.getUrl());
            }
        }

        bookmark.update(requestDto.getTitle(), requestDto.getUrl(), requestDto.getDescription());
        bookmark.setFolder(resolveFolder(requestDto.getFolderId()));

        // Refresh favicon if URL changed
        if (!bookmark.getUrl().equals(requestDto.getUrl())) {
            Map<String, String> metadata = metadataExtractor.extract(requestDto.getUrl());
            bookmark.setFavicon(metadata.getOrDefault("favicon", ""));
        }

        // Clear existing tags and reassign
        new HashSet<>(bookmark.getTags()).forEach(bookmark::removeTag);
        assignTags(bookmark, requestDto.getTagNames());

        auditLogService.log(currentUser.getUsername(), "UPDATE_BOOKMARK", "Bookmark", id, requestDto.getTitle());
        return BookmarkResponseDto.from(bookmark);
    }

    @Transactional
    public void delete(User currentUser, Long id) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + id));

        // Verify ownership
        if (!isAdmin(currentUser) && (bookmark.getUser() == null || !bookmark.getUser().getId().equals(currentUser.getId()))) {
            throw new SecurityException("Access denied");
        }

        bookmarkRepository.delete(bookmark);
        auditLogService.log(currentUser.getUsername(), "DELETE_BOOKMARK", "Bookmark", id, bookmark.getTitle());
    }

    // --- Access tracking ---

    @Transactional
    public BookmarkResponseDto recordAccess(Long id) {
        Bookmark bookmark = bookmarkRepository.findWithTagsAndFolderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found with id: " + id));
        bookmark.recordAccess();
        return BookmarkResponseDto.from(bookmark);
    }

    public List<BookmarkResponseDto> findFrequentlyAccessed(User currentUser, int limit) {
        if (isAdmin(currentUser)) {
            return bookmarkRepository.findTopByAccessCount(PageRequest.of(0, limit)).stream()
                    .map(BookmarkResponseDto::from)
                    .collect(Collectors.toList());
        }
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
        if (isAdmin(currentUser)) {
            return bookmarkRepository.searchByKeyword(keyword, pageable)
                    .map(BookmarkResponseDto::from);
        }
        return bookmarkRepository.searchByKeywordAndUserId(currentUser.getId(), keyword, pageable)
                .map(BookmarkResponseDto::from);
    }

    public List<BookmarkResponseDto> findByTagName(User currentUser, String tagName) {
        if (isAdmin(currentUser)) {
            return bookmarkRepository.findByTagName(tagName).stream()
                    .map(BookmarkResponseDto::from)
                    .collect(Collectors.toList());
        }
        return bookmarkRepository.findByUserIdAndTagName(currentUser.getId(), tagName).stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<BookmarkResponseDto> findByFolderId(User currentUser, Long folderId) {
        if (isAdmin(currentUser)) {
            return bookmarkRepository.findByFolderIdWithTags(folderId).stream()
                    .map(BookmarkResponseDto::from)
                    .collect(Collectors.toList());
        }
        return bookmarkRepository.findByUserIdAndFolderId(currentUser.getId(), folderId).stream()
                .map(BookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<BookmarkResponseDto> findUncategorized(User currentUser) {
        if (isAdmin(currentUser)) {
            return bookmarkRepository.findByFolderIsNullWithTags().stream()
                    .map(BookmarkResponseDto::from)
                    .collect(Collectors.toList());
        }
        return bookmarkRepository.findByUserIdAndFolderIsNull(currentUser.getId()).stream()
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

        bookmark.setFolder(resolveFolder(folderId));
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
}
