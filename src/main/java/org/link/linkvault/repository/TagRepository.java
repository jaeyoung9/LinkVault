package org.link.linkvault.repository;

import org.link.linkvault.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT DISTINCT t FROM Tag t LEFT JOIN FETCH t.bookmarks")
    List<Tag> findAllWithBookmarks();

    @Query("SELECT t FROM Tag t WHERE t.bookmarks IS EMPTY")
    List<Tag> findUnusedTags();

    @Query("SELECT t FROM Tag t LEFT JOIN t.bookmarks b GROUP BY t ORDER BY COUNT(b) DESC")
    List<Tag> findAllOrderByBookmarkCountDesc();

    List<Tag> findByNameContainingIgnoreCase(String name);
}
