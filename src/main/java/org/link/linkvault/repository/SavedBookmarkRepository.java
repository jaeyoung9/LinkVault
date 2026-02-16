package org.link.linkvault.repository;

import org.link.linkvault.entity.SavedBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedBookmarkRepository extends JpaRepository<SavedBookmark, Long> {

    Optional<SavedBookmark> findByUserIdAndBookmarkId(Long userId, Long bookmarkId);

    boolean existsByUserIdAndBookmarkId(Long userId, Long bookmarkId);

    @Query("SELECT sb FROM SavedBookmark sb LEFT JOIN FETCH sb.bookmark b LEFT JOIN FETCH b.tags LEFT JOIN FETCH b.folder WHERE sb.user.id = :userId ORDER BY sb.createdAt DESC")
    List<SavedBookmark> findByUserIdWithBookmark(@Param("userId") Long userId);

    long countByUserId(Long userId);

    @Query("SELECT sb.bookmark.id FROM SavedBookmark sb WHERE sb.user.id = :userId AND sb.bookmark.id IN :bookmarkIds")
    List<Long> findBookmarkIdsByUserIdAndBookmarkIdIn(@Param("userId") Long userId, @Param("bookmarkIds") List<Long> bookmarkIds);

    void deleteByUserId(Long userId);

    void deleteByBookmarkId(Long bookmarkId);
}
