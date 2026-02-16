package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.FavoriteBookmarkResponseDto;
import org.link.linkvault.dto.FavoriteReorderDto;
import org.link.linkvault.entity.Bookmark;
import org.link.linkvault.entity.FavoriteBookmark;
import org.link.linkvault.entity.User;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.BookmarkRepository;
import org.link.linkvault.repository.FavoriteBookmarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteBookmarkService {

    private final FavoriteBookmarkRepository favoriteBookmarkRepository;
    private final BookmarkRepository bookmarkRepository;
    private final NotificationService notificationService;

    @Transactional
    public boolean toggleFavorite(User user, Long bookmarkId) {
        Optional<FavoriteBookmark> existing = favoriteBookmarkRepository.findByUserIdAndBookmarkId(user.getId(), bookmarkId);
        if (existing.isPresent()) {
            favoriteBookmarkRepository.delete(existing.get());
            return false;
        } else {
            Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found: " + bookmarkId));
            int nextOrder = (int) favoriteBookmarkRepository.countByUserId(user.getId());
            favoriteBookmarkRepository.save(new FavoriteBookmark(user, bookmark, nextOrder));
            notificationService.notifyFavorite(bookmark, user);
            return true;
        }
    }

    public List<FavoriteBookmarkResponseDto> getFavorites(User user) {
        return favoriteBookmarkRepository.findByUserIdOrderByDisplayOrder(user.getId()).stream()
                .map(FavoriteBookmarkResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void reorder(User user, FavoriteReorderDto dto) {
        for (FavoriteReorderDto.Item item : dto.getItems()) {
            favoriteBookmarkRepository.findByUserIdAndBookmarkId(user.getId(), item.getBookmarkId())
                    .ifPresent(fb -> fb.setDisplayOrder(item.getDisplayOrder()));
        }
    }

    public List<Long> getFavoritedBookmarkIds(User user, List<Long> bookmarkIds) {
        if (bookmarkIds.isEmpty()) return List.of();
        return favoriteBookmarkRepository.findBookmarkIdsByUserIdAndBookmarkIdIn(user.getId(), bookmarkIds);
    }
}
