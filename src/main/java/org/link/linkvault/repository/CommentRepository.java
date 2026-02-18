package org.link.linkvault.repository;

import org.link.linkvault.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.bookmark.id = :bookmarkId ORDER BY c.createdAt ASC")
    List<Comment> findAllByBookmarkId(@Param("bookmarkId") Long bookmarkId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.bookmark.id = :bookmarkId AND c.parent IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findTopLevelByBookmarkId(@Param("bookmarkId") Long bookmarkId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.user JOIN FETCH c.bookmark ORDER BY c.createdAt DESC")
    List<Comment> findAllWithUserAndBookmark();

    long countByBookmarkId(Long bookmarkId);

    List<Comment> findByUserId(Long userId);

    void deleteByBookmarkId(Long bookmarkId);

    @Modifying
    @Query("UPDATE Comment c SET c.parent = null WHERE c.parent.id IN (SELECT c2.id FROM Comment c2 WHERE c2.user.id = :userId)")
    void detachRepliesFromUserComments(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Comment c SET c.parent = null WHERE c.parent.id = :parentId")
    void detachRepliesFromComment(@Param("parentId") Long parentId);

    void deleteByUserId(Long userId);
}
