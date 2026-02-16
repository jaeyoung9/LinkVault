package org.link.linkvault.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qna_feedback", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "qna_article_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qna_article_id", nullable = false)
    private QnaArticle qnaArticle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean helpful;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public QnaFeedback(QnaArticle qnaArticle, User user, boolean helpful) {
        this.qnaArticle = qnaArticle;
        this.user = user;
        this.helpful = helpful;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void changeFeedback(boolean helpful) {
        this.helpful = helpful;
    }
}
