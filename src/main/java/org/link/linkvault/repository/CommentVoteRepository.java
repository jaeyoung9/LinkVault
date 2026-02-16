package org.link.linkvault.repository;

import org.link.linkvault.entity.CommentVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {

    Optional<CommentVote> findByUserIdAndCommentId(Long userId, Long commentId);

    List<CommentVote> findByUserIdAndCommentIdIn(Long userId, List<Long> commentIds);
}
