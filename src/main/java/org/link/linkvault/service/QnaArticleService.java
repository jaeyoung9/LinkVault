package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.QnaArticleRequestDto;
import org.link.linkvault.dto.QnaArticleResponseDto;
import org.link.linkvault.dto.QnaFeedbackResponseDto;
import org.link.linkvault.entity.QnaArticle;
import org.link.linkvault.entity.QnaFeedback;
import org.link.linkvault.entity.QnaStatus;
import org.link.linkvault.entity.User;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.QnaArticleRepository;
import org.link.linkvault.repository.QnaFeedbackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaArticleService {

    private final QnaArticleRepository qnaArticleRepository;
    private final QnaFeedbackRepository qnaFeedbackRepository;
    private final AuditLogService auditLogService;

    public List<QnaArticleResponseDto> findAllPublished() {
        return qnaArticleRepository.findAllPublished().stream()
                .map(QnaArticleResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<QnaArticleResponseDto> findPublishedByCategory(String category) {
        return qnaArticleRepository.findPublishedByCategory(category).stream()
                .map(QnaArticleResponseDto::from)
                .collect(Collectors.toList());
    }

    public Page<QnaArticleResponseDto> searchPublished(String keyword, Pageable pageable) {
        return qnaArticleRepository.searchPublished(keyword, pageable)
                .map(QnaArticleResponseDto::from);
    }

    public List<String> getPublishedCategories() {
        return qnaArticleRepository.findDistinctPublishedCategories();
    }

    public QnaArticleResponseDto findPublishedById(Long id) {
        QnaArticle article = qnaArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QnA article not found: " + id));
        if (article.getStatus() != QnaStatus.PUBLISHED) {
            throw new ResourceNotFoundException("QnA article not found: " + id);
        }
        return QnaArticleResponseDto.from(article);
    }

    public boolean hasRecentlyUpdatedArticles() {
        return qnaArticleRepository.countRecentlyUpdatedPublished(LocalDateTime.now().minusDays(7)) > 0;
    }

    @Transactional
    public QnaArticleResponseDto create(QnaArticleRequestDto dto, User creator, String actorUsername) {
        QnaArticle article = QnaArticle.builder()
                .question(dto.getQuestion())
                .answer(dto.getAnswer())
                .category(dto.getCategory())
                .tags(dto.getTags())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .status(dto.getStatus() != null ? dto.getStatus() : QnaStatus.DRAFT)
                .relatedLinks(dto.getRelatedLinks())
                .createdBy(creator)
                .build();
        QnaArticleResponseDto result = QnaArticleResponseDto.from(qnaArticleRepository.save(article));
        auditLogService.log(actorUsername, AuditActionCodes.QNA_CREATE, "QnaArticle", result.getId(),
                AuditDetailFormatter.format("category", dto.getCategory()));
        return result;
    }

    @Transactional
    public QnaArticleResponseDto update(Long id, QnaArticleRequestDto dto, String actorUsername) {
        QnaArticle article = qnaArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QnA article not found: " + id));
        article.update(dto.getQuestion(), dto.getAnswer(), dto.getCategory(), dto.getTags(), dto.getRelatedLinks());
        if (dto.getDisplayOrder() != null) {
            article.setDisplayOrder(dto.getDisplayOrder());
        }
        article.incrementVersion();
        auditLogService.log(actorUsername, AuditActionCodes.QNA_UPDATE, "QnaArticle", id,
                AuditDetailFormatter.format("category", dto.getCategory()));
        return QnaArticleResponseDto.from(article);
    }

    @Transactional
    public void updateStatus(Long id, QnaStatus status, String actorUsername) {
        QnaArticle article = qnaArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QnA article not found: " + id));
        article.updateStatus(status);
        auditLogService.log(actorUsername, AuditActionCodes.QNA_STATUS_CHANGE, "QnaArticle", id,
                AuditDetailFormatter.format("status", String.valueOf(status)));
    }

    @Transactional
    public void delete(Long id, String actorUsername) {
        if (!qnaArticleRepository.existsById(id)) {
            throw new ResourceNotFoundException("QnA article not found: " + id);
        }
        qnaFeedbackRepository.deleteByQnaArticleId(id);
        qnaArticleRepository.deleteById(id);
        auditLogService.log(actorUsername, AuditActionCodes.QNA_DELETE, "QnaArticle", id, null);
    }

    public Page<QnaArticleResponseDto> findAllAdmin(Pageable pageable) {
        return qnaArticleRepository.findAllWithCreator(pageable)
                .map(QnaArticleResponseDto::from);
    }

    public QnaArticleResponseDto findByIdAdmin(Long id) {
        QnaArticle article = qnaArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QnA article not found: " + id));
        return QnaArticleResponseDto.from(article);
    }

    public QnaFeedbackResponseDto getFeedback(Long articleId, User user) {
        QnaArticle article = qnaArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("QnA article not found: " + articleId));
        Optional<QnaFeedback> feedback = qnaFeedbackRepository.findByUserIdAndQnaArticleId(user.getId(), articleId);
        return QnaFeedbackResponseDto.builder()
                .articleId(articleId)
                .userFeedback(feedback.map(QnaFeedback::isHelpful).orElse(null))
                .helpfulCount(article.getHelpfulCount())
                .notHelpfulCount(article.getNotHelpfulCount())
                .build();
    }

    @Transactional
    public QnaFeedbackResponseDto submitFeedback(Long articleId, User user, boolean helpful) {
        QnaArticle article = qnaArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("QnA article not found: " + articleId));
        Optional<QnaFeedback> existing = qnaFeedbackRepository.findByUserIdAndQnaArticleId(user.getId(), articleId);

        if (existing.isPresent()) {
            QnaFeedback fb = existing.get();
            if (fb.isHelpful() != helpful) {
                if (fb.isHelpful()) {
                    article.decrementHelpfulCount();
                    article.incrementNotHelpfulCount();
                } else {
                    article.decrementNotHelpfulCount();
                    article.incrementHelpfulCount();
                }
                fb.changeFeedback(helpful);
            }
        } else {
            qnaFeedbackRepository.save(new QnaFeedback(article, user, helpful));
            if (helpful) {
                article.incrementHelpfulCount();
            } else {
                article.incrementNotHelpfulCount();
            }
        }

        return QnaFeedbackResponseDto.builder()
                .articleId(articleId)
                .userFeedback(helpful)
                .helpfulCount(article.getHelpfulCount())
                .notHelpfulCount(article.getNotHelpfulCount())
                .build();
    }
}
