package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.QnaArticle;
import org.link.linkvault.entity.QnaStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class QnaArticleResponseDto {
    private Long id;
    private String question;
    private String answer;
    private String category;
    private String tags;
    private int displayOrder;
    private int version;
    private QnaStatus status;
    private String relatedLinks;
    private String createdByUsername;
    private int helpfulCount;
    private int notHelpfulCount;
    private boolean recentlyUpdated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static QnaArticleResponseDto from(QnaArticle a) {
        return QnaArticleResponseDto.builder()
                .id(a.getId())
                .question(a.getQuestion())
                .answer(a.getAnswer())
                .category(a.getCategory())
                .tags(a.getTags())
                .displayOrder(a.getDisplayOrder())
                .version(a.getVersion())
                .status(a.getStatus())
                .relatedLinks(a.getRelatedLinks())
                .createdByUsername(a.getCreatedBy() != null ? a.getCreatedBy().getUsername() : null)
                .helpfulCount(a.getHelpfulCount())
                .notHelpfulCount(a.getNotHelpfulCount())
                .recentlyUpdated(a.isRecentlyUpdated())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
