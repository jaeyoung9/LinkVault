package org.link.linkvault.repository;

import org.link.linkvault.entity.QnaFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QnaFeedbackRepository extends JpaRepository<QnaFeedback, Long> {

    Optional<QnaFeedback> findByUserIdAndQnaArticleId(Long userId, Long qnaArticleId);

    void deleteByQnaArticleId(Long qnaArticleId);

    void deleteByUserId(Long userId);
}
