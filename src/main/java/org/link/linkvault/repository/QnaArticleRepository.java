package org.link.linkvault.repository;

import org.link.linkvault.entity.QnaArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QnaArticleRepository extends JpaRepository<QnaArticle, Long> {

    @Query("SELECT q FROM QnaArticle q LEFT JOIN FETCH q.createdBy " +
           "WHERE q.status = 'PUBLISHED' ORDER BY q.displayOrder ASC")
    List<QnaArticle> findAllPublished();

    @Query("SELECT q FROM QnaArticle q LEFT JOIN FETCH q.createdBy " +
           "WHERE q.status = 'PUBLISHED' AND q.category = :category " +
           "ORDER BY q.displayOrder ASC")
    List<QnaArticle> findPublishedByCategory(@Param("category") String category);

    @Query(value = "SELECT q FROM QnaArticle q LEFT JOIN FETCH q.createdBy " +
           "WHERE q.status = 'PUBLISHED' AND (" +
           "LOWER(q.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(q.answer) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(q.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(q) FROM QnaArticle q WHERE q.status = 'PUBLISHED' AND (" +
           "LOWER(q.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(q.answer) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<QnaArticle> searchPublished(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT q.category FROM QnaArticle q WHERE q.status = 'PUBLISHED' ORDER BY q.category")
    List<String> findDistinctPublishedCategories();

    @Query(value = "SELECT q FROM QnaArticle q LEFT JOIN FETCH q.createdBy ORDER BY q.createdAt DESC",
           countQuery = "SELECT COUNT(q) FROM QnaArticle q")
    Page<QnaArticle> findAllWithCreator(Pageable pageable);

    @Query("SELECT COUNT(q) FROM QnaArticle q WHERE q.status = 'PUBLISHED' AND q.updatedAt > :since")
    long countRecentlyUpdatedPublished(@Param("since") LocalDateTime since);
}
