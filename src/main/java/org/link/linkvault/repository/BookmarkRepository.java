package org.link.linkvault.repository;

import org.link.linkvault.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findByTitleContainingIgnoreCase(String keyword);

    @Query("SELECT DISTINCT b FROM Bookmark b JOIN b.tags t WHERE t.name = :tagName")
    List<Bookmark> findByTagName(@Param("tagName") String tagName);

    @Query("SELECT DISTINCT b FROM Bookmark b JOIN b.tags t WHERE t.name IN :tagNames")
    List<Bookmark> findByTagNames(@Param("tagNames") List<String> tagNames);

    boolean existsByUrl(String url);

    @Query("SELECT b FROM Bookmark b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Bookmark> searchByKeyword(@Param("keyword") String keyword);
}
