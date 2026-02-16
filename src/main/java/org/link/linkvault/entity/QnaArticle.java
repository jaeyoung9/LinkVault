package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qna_articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(nullable = false, columnDefinition = "CLOB")
    private String answer;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 500)
    private String tags;

    @Column(nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private int version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QnaStatus status = QnaStatus.DRAFT;

    @Column(length = 2000)
    private String relatedLinks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private int helpfulCount = 0;

    @Column(nullable = false)
    private int notHelpfulCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public QnaArticle(String question, String answer, String category, String tags,
                      int displayOrder, QnaStatus status, String relatedLinks, User createdBy) {
        this.question = question;
        this.answer = answer;
        this.category = category;
        this.tags = tags;
        this.displayOrder = displayOrder;
        this.status = status != null ? status : QnaStatus.DRAFT;
        this.relatedLinks = relatedLinks;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String question, String answer, String category, String tags, String relatedLinks) {
        this.question = question;
        this.answer = answer;
        this.category = category;
        this.tags = tags;
        this.relatedLinks = relatedLinks;
    }

    public void updateStatus(QnaStatus status) {
        this.status = status;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void incrementVersion() {
        this.version++;
    }

    public void incrementHelpfulCount() {
        this.helpfulCount++;
    }

    public void decrementHelpfulCount() {
        if (this.helpfulCount > 0) this.helpfulCount--;
    }

    public void incrementNotHelpfulCount() {
        this.notHelpfulCount++;
    }

    public void decrementNotHelpfulCount() {
        if (this.notHelpfulCount > 0) this.notHelpfulCount--;
    }

    public boolean isRecentlyUpdated() {
        return updatedAt != null && updatedAt.isAfter(LocalDateTime.now().minusDays(7));
    }
}
