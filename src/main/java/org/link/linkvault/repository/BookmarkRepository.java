package org.link.linkvault.repository;

import org.link.linkvault.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // --- Fetch-join queries to prevent N+1 (non-paginated) ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags LEFT JOIN FETCH b.folder")
    List<Bookmark> findAllWithTagsAndFolder();

    @EntityGraph(attributePaths = {"tags", "folder"})
    Optional<Bookmark> findWithTagsAndFolderById(Long id);

    // --- Paginated listing (batch_fetch_size handles tags N+1, folder is ManyToOne so eager-safe) ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b")
    Page<Bookmark> findAllWithTagsAndFolder(Pageable pageable);

    // --- User-scoped paginated listing ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId")
    Page<Bookmark> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // --- Full-text search with pagination ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b " +
            "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Bookmark> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // --- User-scoped search ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b " +
            "WHERE b.user.id = :userId AND (" +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Bookmark> searchByKeywordAndUserId(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    // --- Filter by tag with fetch join ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags LEFT JOIN FETCH b.folder " +
            "JOIN b.tags t WHERE t.name = :tagName")
    List<Bookmark> findByTagName(@Param("tagName") String tagName);

    // --- User-scoped tag filter ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags LEFT JOIN FETCH b.folder " +
            "JOIN b.tags t WHERE b.user.id = :userId AND t.name = :tagName")
    List<Bookmark> findByUserIdAndTagName(@Param("userId") Long userId, @Param("tagName") String tagName);

    // --- Filter by folder ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags WHERE b.folder.id = :folderId")
    List<Bookmark> findByFolderIdWithTags(@Param("folderId") Long folderId);

    // --- User-scoped folder filter ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags WHERE b.user.id = :userId AND b.folder.id = :folderId")
    List<Bookmark> findByUserIdAndFolderId(@Param("userId") Long userId, @Param("folderId") Long folderId);

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags WHERE b.folder IS NULL")
    List<Bookmark> findByFolderIsNullWithTags();

    // --- User-scoped uncategorized ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags WHERE b.user.id = :userId AND b.folder IS NULL")
    List<Bookmark> findByUserIdAndFolderIsNull(@Param("userId") Long userId);

    // --- Duplicate URL detection ---

    boolean existsByUrl(String url);

    boolean existsByUrlAndUserId(String url, Long userId);

    @Query("SELECT b FROM Bookmark b WHERE b.url = :url")
    Optional<Bookmark> findByUrl(@Param("url") String url);

    // --- Frequently accessed (batch_fetch_size handles tags) ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b ORDER BY b.accessCount DESC")
    List<Bookmark> findTopByAccessCount(Pageable pageable);

    // --- User-scoped frequently accessed ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId ORDER BY b.accessCount DESC")
    List<Bookmark> findTopByAccessCountAndUserId(@Param("userId") Long userId, Pageable pageable);

    // --- Recently accessed (batch_fetch_size handles tags) ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b WHERE b.lastAccessedAt IS NOT NULL ORDER BY b.lastAccessedAt DESC")
    List<Bookmark> findRecentlyAccessed(Pageable pageable);

    // --- Admin stats ---

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
