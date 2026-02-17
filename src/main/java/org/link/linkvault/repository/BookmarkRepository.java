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

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags LEFT JOIN FETCH b.folder WHERE b.deleted = false AND b.privatePost = false")
    List<Bookmark> findAllWithTagsAndFolder();

    @EntityGraph(attributePaths = {"tags", "folder"})
    Optional<Bookmark> findWithTagsAndFolderById(Long id);

    // --- Paginated listing (batch_fetch_size handles tags N+1, folder is ManyToOne so eager-safe) ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b WHERE b.deleted = false AND b.privatePost = false")
    Page<Bookmark> findAllWithTagsAndFolder(Pageable pageable);

    // --- User-scoped paginated listing ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId AND b.deleted = false")
    Page<Bookmark> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // --- Full-text search with pagination ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b " +
            "WHERE b.deleted = false AND b.privatePost = false AND (" +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Bookmark> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // --- User-scoped search ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b " +
            "WHERE b.deleted = false AND b.user.id = :userId AND (" +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Bookmark> searchByKeywordAndUserId(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    // --- Filter by tag with fetch join ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags LEFT JOIN FETCH b.folder " +
            "JOIN b.tags t WHERE b.deleted = false AND b.privatePost = false AND t.name = :tagName")
    List<Bookmark> findByTagName(@Param("tagName") String tagName);

    // --- User-scoped tag filter ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags LEFT JOIN FETCH b.folder " +
            "JOIN b.tags t WHERE b.deleted = false AND b.user.id = :userId AND t.name = :tagName")
    List<Bookmark> findByUserIdAndTagName(@Param("userId") Long userId, @Param("tagName") String tagName);

    // --- Filter by folder ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags WHERE b.deleted = false AND b.privatePost = false AND b.folder.id = :folderId")
    List<Bookmark> findByFolderIdWithTags(@Param("folderId") Long folderId);

    // --- User-scoped folder filter ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags WHERE b.deleted = false AND b.user.id = :userId AND b.folder.id = :folderId")
    List<Bookmark> findByUserIdAndFolderId(@Param("userId") Long userId, @Param("folderId") Long folderId);

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags WHERE b.deleted = false AND b.privatePost = false AND b.folder IS NULL")
    List<Bookmark> findByFolderIsNullWithTags();

    // --- User-scoped uncategorized ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags WHERE b.deleted = false AND b.user.id = :userId AND b.folder IS NULL")
    List<Bookmark> findByUserIdAndFolderIsNull(@Param("userId") Long userId);

    // --- Duplicate URL detection ---

    boolean existsByUrl(String url);

    boolean existsByUrlAndUserId(String url, Long userId);

    @Query("SELECT b FROM Bookmark b WHERE b.url = :url")
    Optional<Bookmark> findByUrl(@Param("url") String url);

    // --- Frequently accessed (batch_fetch_size handles tags) ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b WHERE b.deleted = false AND b.privatePost = false ORDER BY b.accessCount DESC")
    List<Bookmark> findTopByAccessCount(Pageable pageable);

    // --- User-scoped frequently accessed ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b WHERE b.deleted = false AND b.user.id = :userId ORDER BY b.accessCount DESC")
    List<Bookmark> findTopByAccessCountAndUserId(@Param("userId") Long userId, Pageable pageable);

    // --- Recently accessed (batch_fetch_size handles tags) ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b WHERE b.deleted = false AND b.privatePost = false AND b.lastAccessedAt IS NOT NULL ORDER BY b.lastAccessedAt DESC")
    List<Bookmark> findRecentlyAccessed(Pageable pageable);

    // --- Admin stats ---

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Bookmark> findAllByUserId(Long userId);

    // --- Map discovery: posts with location ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags LEFT JOIN FETCH b.folder " +
            "WHERE b.deleted = false AND b.privatePost = false AND b.latitude IS NOT NULL AND b.longitude IS NOT NULL")
    List<Bookmark> findAllWithLocation();

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags LEFT JOIN FETCH b.folder " +
            "WHERE b.deleted = false AND b.latitude IS NOT NULL AND b.longitude IS NOT NULL")
    List<Bookmark> findAllWithLocationAdmin();

    // --- Admin listing (includes private posts) ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b WHERE b.deleted = false")
    Page<Bookmark> findAllIncludingPrivate(Pageable pageable);

    // --- Private posts ---

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags LEFT JOIN FETCH b.folder " +
            "WHERE b.deleted = false AND b.privatePost = true AND b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Bookmark> findPrivateByUserId(@Param("userId") Long userId);

    // --- Deleted bookmarks (soft-delete support) ---

    @EntityGraph(attributePaths = {"folder"})
    @Query("SELECT b FROM Bookmark b WHERE b.deleted = true")
    Page<Bookmark> findAllDeletedBookmarks(Pageable pageable);

    @Query("SELECT DISTINCT b FROM Bookmark b LEFT JOIN FETCH b.tags LEFT JOIN FETCH b.folder WHERE b.deleted = true AND b.user.id = :userId")
    List<Bookmark> findAllDeletedByUserId(@Param("userId") Long userId);
}
