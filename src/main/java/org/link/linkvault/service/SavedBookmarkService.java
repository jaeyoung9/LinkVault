package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.BookmarkResponseDto;
import org.link.linkvault.entity.Bookmark;
import org.link.linkvault.entity.SavedBookmark;
import org.link.linkvault.entity.User;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.BookmarkRepository;
import org.link.linkvault.repository.SavedBookmarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedBookmarkService {

    private final SavedBookmarkRepository savedBookmarkRepository;
    private final BookmarkRepository bookmarkRepository;
    private final NotificationService notificationService;

    @Transactional
    public boolean toggleSave(User user, Long bookmarkId) {
        Optional<SavedBookmark> existing = savedBookmarkRepository.findByUserIdAndBookmarkId(user.getId(), bookmarkId);
        if (existing.isPresent()) {
            savedBookmarkRepository.delete(existing.get());
            return false;
        } else {
            Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found: " + bookmarkId));
            savedBookmarkRepository.save(new SavedBookmark(user, bookmark));
            notificationService.notifySave(bookmark, user);
            return true;
        }
    }

    public List<BookmarkResponseDto> getSavedBookmarks(User user) {
        return savedBookmarkRepository.findByUserIdWithBookmark(user.getId()).stream()
                .map(sb -> BookmarkResponseDto.from(sb.getBookmark()))
                .collect(Collectors.toList());
    }

    public long getCount(User user) {
        return savedBookmarkRepository.countByUserId(user.getId());
    }

    public boolean isSaved(User user, Long bookmarkId) {
        return savedBookmarkRepository.existsByUserIdAndBookmarkId(user.getId(), bookmarkId);
    }

    public List<Long> getSavedBookmarkIds(User user, List<Long> bookmarkIds) {
        if (bookmarkIds.isEmpty()) return List.of();
        return savedBookmarkRepository.findBookmarkIdsByUserIdAndBookmarkIdIn(user.getId(), bookmarkIds);
    }
}
