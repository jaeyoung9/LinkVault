package org.link.linkvault.repository;

import org.link.linkvault.entity.FavoriteBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteBookmarkRepository extends JpaRepository<FavoriteBookmark, Long> {

    @Query("SELECT fb FROM FavoriteBookmark fb LEFT JOIN FETCH fb.bookmark b LEFT JOIN FETCH b.tags LEFT JOIN FETCH b.folder WHERE fb.user.id = :userId ORDER BY fb.displayOrder ASC")
    List<FavoriteBookmark> findByUserIdOrderByDisplayOrder(@Param("userId") Long userId);

    Optional<FavoriteBookmark> findByUserIdAndBookmarkId(Long userId, Long bookmarkId);

    long countByUserId(Long userId);

    boolean existsByUserIdAndBookmarkId(Long userId, Long bookmarkId);

    @Query("SELECT fb.bookmark.id FROM FavoriteBookmark fb WHERE fb.user.id = :userId AND fb.bookmark.id IN :bookmarkIds")
    List<Long> findBookmarkIdsByUserIdAndBookmarkIdIn(@Param("userId") Long userId, @Param("bookmarkIds") List<Long> bookmarkIds);
}
